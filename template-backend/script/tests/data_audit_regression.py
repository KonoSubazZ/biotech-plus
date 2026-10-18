"""真实MySQL 8与Spring Boot数据审计验收，使用随机隔离库，不修改源业务数据。

预期输出 DATA_AUDIT_API: PASS；设置 AUDIT_TEST_UI=1 时另输出 DATA_AUDIT_UI: PASS。
边界：跨药企详情被拒绝；审计写入失败业务共同回滚；一个样本两个分析批次独立可查。
运行前构建：mvn -Paudit-verification -Dprofiles.active=local -DskipTests=false package
密码从容器环境读取，验收用户密码仅运行时生成，不写入文件或终端。
"""
import json
import os
from pathlib import Path
import re
import secrets
import shlex
import shutil
import socket
import subprocess
import sys
import time

import bcrypt
import requests

ROOT = Path(__file__).resolve().parents[3]
BACKEND = ROOT / 'template-backend'
CONTAINER = os.environ.get('AUDIT_TEST_MYSQL_CONTAINER', 'codex-report-local-mysql')
DATABASE = 'codex_audit_test_' + secrets.token_hex(4)
SOURCE_DATABASE = os.environ.get('AUDIT_TEST_SOURCE_DATABASE', 'ry-vue')
COUNT = 0
CLIENT = ''
BASE = ''


def check(value, label):
    global COUNT
    assert value, label
    COUNT += 1
    print('PASS:', label, flush=True)


def inspect(name):
    return json.loads(subprocess.check_output(['docker', 'inspect', name], encoding='utf-8'))[0]


def mysql(sql, database=DATABASE):
    assert re.fullmatch(r'[\w-]+', database), 'invalid fixture database'
    command = ['docker', 'exec', '-i', CONTAINER, 'sh', '-c',
               'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql -uroot '
               '--default-character-set=utf8mb4 --batch --skip-column-names']
    result = subprocess.run(command, input='USE `' + database + '`;\n' + sql,
                            capture_output=True, encoding='utf-8', timeout=60)
    if result.returncode:
        raise AssertionError(result.stderr)
    return result.stdout.strip()


def quote(value):
    if value is None:
        return 'NULL'
    return "'" + str(value).replace('\\', '\\\\').replace("'", "''") + "'"


def available_port():
    with socket.socket() as candidate:
        candidate.bind(('localhost', 0))
        return candidate.getsockname()[1]


def call(method, path, token=None, **kwargs):
    headers = {'clientid': CLIENT}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    result = requests.request(method, BASE + path, headers=headers, timeout=25, **kwargs)
    return result.json()


def success(result, label):
    check(result.get('code') == 200, label + ': ' + str(result.get('msg', '')))
    return result.get('data')


def login(name, password):
    return success(call('POST', '/auth/login', json={'clientId': CLIENT, 'grantType': 'password',
                   'username': name, 'password': password}), 'login ' + name)['access_token']


def fixture_schema(password):
    mysql('CREATE DATABASE `' + DATABASE + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;', 'mysql')
    check(mysql('SELECT VERSION();').startswith('8.'), 'real MySQL 8 runtime')
    tables = mysql('SELECT table_name FROM information_schema.tables WHERE table_schema='
                   + quote(SOURCE_DATABASE) + " AND table_type='BASE TABLE';").splitlines()
    for table in tables:
        assert re.fullmatch(r'\w+', table), 'invalid source table'
        if table in ('audit_event', 'audit_change'):
            continue
        mysql('CREATE TABLE `' + table + '` LIKE `' + SOURCE_DATABASE + '`.`' + table + '`;')
        if table.startswith('sys_') and table not in ('sys_oper_log', 'sys_logininfor', 'sys_oss', 'sys_social'):
            condition = ' WHERE user_id = 1' if table in ('sys_user', 'sys_user_role') else ''
            mysql('INSERT INTO `' + table + '` SELECT * FROM `' + SOURCE_DATABASE + '`.`' + table + '`' + condition + ';')
    migration = (BACKEND / 'script/sql/update/update_5.6.28-data-audit.sql').read_text('utf-8')
    mysql(migration)
    mysql(migration)
    mysql('DELETE FROM sys_role_menu WHERE role_id NOT IN (SELECT role_id FROM sys_role);')
    check(mysql("SELECT COUNT(*) FROM sys_menu WHERE perms='compliance:dataAudit:list';") == '1',
          'audit migration is repeatable and menu is unique')
    hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()
    mysql('UPDATE sys_user SET password=' + quote(hashed) + ' WHERE user_id=1;')


def fixture_row(table, values):
    assert re.fullmatch(r'\w+', table)
    required = mysql('SELECT column_name,data_type FROM information_schema.columns WHERE table_schema=DATABASE()'
                     + ' AND table_name=' + quote(table) + " AND is_nullable='NO' AND column_default IS NULL"
                     + " AND extra NOT LIKE '%auto_increment%';")
    for line in required.splitlines():
        column, kind = line.split('\t')
        if column not in values:
            values[column] = 0 if kind in ('bigint', 'int', 'tinyint', 'decimal', 'double', 'float') else ''
    columns = ','.join('`' + key + '`' for key in values)
    sql = 'INSERT INTO `' + table + '` (' + columns + ') VALUES ('
    sql += ','.join(quote(value) for value in values.values()) + '); SELECT LAST_INSERT_ID();'
    return int(mysql(sql))


def verify_tenant_changes(admin, tenant_id):
    key = int(mysql('SELECT id FROM sys_tenant WHERE tenant_id=' + quote(tenant_id)))
    body = {'id': key, 'tenantId': tenant_id, 'tenantName': '审计验收药企0', 'status': '0'}
    for status, previous in [('1', '0'), ('0', '1')]:
        body['status'] = status
        success(call('PUT', '/system/tenant', admin, json=body), 'change audited tenant status to ' + status)
        events = call('GET', '/compliance/dataAudit/list', admin,
                      params={'action': '药企修改', 'auditScope': 'NON_SAMPLE'})['rows']
        event = next(row for row in events if row['tenantId'] == tenant_id)
        check(event.get('subbarcode') is None and event.get('analysisId') is None,
              'tenant audit is non-sample with actual ownership')
        detail = success(call('GET', '/compliance/dataAudit/' + str(event['id']), admin), 'read tenant status audit detail')
        fields = json.loads(detail['changes'][0]['changedFieldsJson'])
        check(detail['changes'][0]['tableName'] == 'sys_tenant'
              and any(item['field'] == 'status' and item['before'] == previous
                      and item['after'] == status for item in fields), 'actual tenant status before and after values')
    body['tenantName'] = '审计验收药企已更新'
    success(call('PUT', '/system/tenant', admin, json=body), 'edit audited tenant name')
    check(mysql("SELECT COUNT(*) FROM audit_event e JOIN audit_change c ON c.event_id=e.id "
                "WHERE e.tenant_id=" + quote(tenant_id) + " AND c.changed_fields_json LIKE '%tenant_name%';") == '2',
          'tenant creation and name edit both retain changes')
    return body


def verify_empty_tenant_deletion(admin, suffix):
    tenant_id = 'aud_empty_' + suffix
    success(call('POST', '/system/tenant', admin,
            json={'tenantId': tenant_id, 'tenantName': '审计删除验收', 'status': '0'}), 'create audited empty tenant')
    key = int(mysql('SELECT id FROM sys_tenant WHERE tenant_id=' + quote(tenant_id)))
    success(call('DELETE', '/system/tenant/' + str(key), admin), 'delete empty tenant despite its audit history')
    check(mysql('SELECT COUNT(*) FROM sys_tenant WHERE id=' + str(key)) == '0', 'empty tenant actually deleted')
    check(mysql('SELECT COUNT(*) FROM audit_event WHERE tenant_id=' + quote(tenant_id)) == '2',
          'tenant creation and deletion audit history survives deletion')


def run_api(password):
    admin = login('admin', password)
    suffix = secrets.token_hex(3)
    tenants = ['aud_a_' + suffix, 'aud_b_' + suffix]
    for index, tenant in enumerate(tenants):
        success(call('POST', '/system/tenant', admin, json={'tenantId': tenant,
                'tenantName': '审计验收药企' + str(index), 'status': '0'}), 'create isolated tenant')
    tenant_body = verify_tenant_changes(admin, tenants[0])
    verify_empty_tenant_deletion(admin, suffix)
    menu_ids = [int(value) for value in mysql("SELECT menu_id FROM sys_menu WHERE menu_type IN ('C','F');").splitlines()]
    role_key = 'audit_test_' + suffix
    success(call('POST', '/system/role', admin, json={'roleName': '审计验收角色', 'roleKey': role_key,
            'roleSort': 99, 'status': '0', 'menuIds': menu_ids, 'menuCheckStrictly': True}), 'create audited role')
    role_id = int(mysql('SELECT role_id FROM sys_role WHERE role_key=' + quote(role_key)))
    tokens = []
    for index, tenant in enumerate(tenants):
        name = 'audit_user_' + str(index) + '_' + suffix
        success(call('POST', '/system/user', admin, json={'userName': name, 'nickName': name,
                'tenantId': tenant, 'password': password, 'roleIds': [role_id], 'status': '0', 'sex': '0'}),
                'create audited user and role relation')
        tokens.append(login(name, password))
    denied = call('PUT', '/system/tenant', tokens[0], json=dict(tenant_body, status='1'))
    check(denied.get('code') != 200, 'ordinary tenant user cannot change tenant status')
    check(mysql('SELECT status FROM sys_tenant WHERE tenant_id=' + quote(tenants[0])) == '0',
          'denied tenant update leaves status unchanged')
    sample = 'AUDIT-SAMPLE-' + suffix
    second_sample = 'AUDIT-OTHER-' + suffix
    for code, tenant, token in zip([sample, second_sample], tenants, tokens):
        success(call('POST', '/report/sampleInfo', token, json={'subbarcode': code,
                'patientName': '审计验收样本', 'customerName': tenant}), 'create audited sample')
    sample_id = int(mysql('SELECT id FROM sample_file WHERE subbarcode=' + quote(sample)))
    success(call('PUT', '/report/sampleInfo', tokens[0], json={'id': sample_id, 'subbarcode': sample,
            'patientName': '审计验收样本已修改'}), 'update sample through real API')
    events = call('GET', '/compliance/dataAudit/list', tokens[0],
                  params={'subbarcode': sample, 'ascending': True, 'pageSize': 200})['rows']
    check(len(events) == 2, 'create and update form one chronological sample history')
    check([row['occurredAt'] for row in events] == sorted(row['occurredAt'] for row in events), 'sample time ordering')
    check(all(row.get('analysisId') is None for row in events), 'sample common events have no invented analysis ID')
    detail = success(call('GET', '/compliance/dataAudit/' + str(events[-1]['id']), tokens[0]), 'read own audit detail')
    changed = json.loads(detail['changes'][0]['changedFieldsJson'])
    check(any(item['field'] == 'patient_name' and item['before'] == '审计验收样本'
              and item['after'] == '审计验收样本已修改' for item in changed), 'actual database before and after values')
    check(call('GET', '/compliance/dataAudit/' + str(events[-1]['id']), tokens[1]).get('code') != 200,
          'cross-tenant audit detail denied')
    check(not call('GET', '/compliance/dataAudit/list', tokens[1], params={'subbarcode': sample})['rows'],
          'cross-tenant sample history hidden')
    rows = call('GET', '/compliance/dataAudit/list', admin, params={'action': '用户新增', 'pageSize': 200})['rows']
    check(len(rows) == 2 and {row['tenantId'] for row in rows} == set(tenants), 'admin user events retain real tenant ownership')
    user_detail = success(call('GET', '/compliance/dataAudit/' + str(rows[0]['id']), admin), 'read user creation audit')
    check({row['tableName'] for row in user_detail['changes']} == {'sys_user', 'sys_user_role'},
          'user and role relation share one business event')
    check('password' not in json.dumps(user_detail).lower(), 'password and hash excluded from all snapshots')
    success(call('POST', '/qc/wetLabQc/receive', admin, json={'items': [{'subbarcode': sample},
            {'subbarcode': second_sample}]}), 'receive two-tenant QC batch')
    rows = call('GET', '/compliance/dataAudit/list', admin, params={'action': '湿实验质控导入', 'pageSize': 200})['rows']
    check(len(rows) == 2 and {row['tenantId'] for row in rows} == set(tenants), 'one QC transaction audits both tenant scopes')
    success(call('POST', '/qc/wetLabQc/receive', admin, json={'items': [{'subbarcode': sample},
            {'subbarcode': second_sample}]}), 'repeat unchanged QC import')
    unchanged = call('GET', '/compliance/dataAudit/list', admin,
                     params={'action': '湿实验质控导入', 'pageSize': 200})['rows']
    check(len(unchanged) == 4 and sum(row['outcome'] == 'NO_CHANGE' for row in unchanged) == 2,
          'each unchanged QC import remains in its sample history')
    ids = []
    for date in ['20261018', '20261019']:
        ids.append(fixture_row('analysis_data', {'subbarcode': sample, 'tenant_id': tenants[0],
                   'product': 'AUDIT-PRODUCT', 'analysis_date': date}))
    file_id = fixture_row('data_file_status', {'subbarcode': sample, 'tenant_id': tenants[0],
                          'analysis_id': ids[0], 'file_path': 'fixture/input-v1', 'file_name': 'input-v1',
                          'analysis_date': '20261018', 'status': 'Loaded'})
    variant = fixture_row('file_Somatic_SNV_Indel', {'file_id': file_id, 'tenant_id': tenants[0], 'gene': 'EGFR'})
    success(call('POST', '/report/interpretation/variant-report-status', tokens[0], json={'analysisId': ids[0],
            'sourceType': 'SNP_INDEL', 'sourceId': variant, 'isReported': 0}), 'change reported variant through real API')
    batch_events = call('GET', '/compliance/dataAudit/list', tokens[0], params={'subbarcode': sample,
                        'analysisId': ids[0], 'includeSampleCommon': False})['rows']
    check(len(batch_events) == 1 and str(batch_events[0]['analysisId']) == str(ids[0]), 'variant event associates exact data batch')
    other = call('GET', '/compliance/dataAudit/list', tokens[0], params={'subbarcode': sample,
                 'analysisId': ids[1], 'includeSampleCommon': False})['rows']
    check(not other, 'second data batch does not inherit first batch interpretation changes')
    other_analysis = fixture_row('analysis_data', {'subbarcode': second_sample, 'tenant_id': tenants[1],
                                 'product': 'AUDIT-PRODUCT', 'analysis_date': '20261018'})
    sign_flow = 'AUDIT-FLOW-' + suffix
    signed_report = fixture_row('analysis_report', {'analysis_id': other_analysis, 'subbarcode': second_sample,
                                'tenant_id': tenants[1], 'report_status': 50, 'report_sign_flow_no': sign_flow})
    audit_count = mysql('SELECT COUNT(*) FROM audit_event;')
    denied = call('POST', '/report/interpretation/sign/callback', tokens[0],
                  json={'flowNo': sign_flow, 'success': False, 'reason': 'QA cross-tenant'})
    check(denied.get('code') != 200, 'cross-tenant signing callback rejected')
    check(mysql('SELECT report_status FROM analysis_report WHERE report_id=' + str(signed_report)) == '50',
          'cross-tenant callback cannot change another tenant report')
    check(mysql('SELECT COUNT(*) FROM audit_event;') == audit_count, 'denied callback cannot insert foreign audit evidence')
    success(call('POST', '/report/interpretation/sign/callback', tokens[1],
            json={'flowNo': sign_flow, 'success': False, 'reason': 'QA own-tenant'}), 'own-tenant signing callback accepted')
    sign_events = call('GET', '/compliance/dataAudit/list', tokens[1],
                       params={'subbarcode': second_sample, 'analysisId': other_analysis})['rows']
    check(any(str(row.get('reportId')) == str(signed_report) and row['action'] == '报告签署失败' for row in sign_events),
          'own-tenant callback creates report-linked audit evidence')
    before = mysql('SELECT COUNT(*) FROM audit_event;')
    mysql("CREATE TRIGGER qa_audit_fail BEFORE INSERT ON audit_change FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='audit unavailable';")
    try:
        response = call('POST', '/report/sampleInfo', tokens[0], json={'subbarcode': 'AUDIT-ROLLBACK-' + suffix})
        check(response.get('code') != 200, 'audit persistence failure rejects business request')
        check(mysql('SELECT COUNT(*) FROM sample_file WHERE subbarcode=' + quote('AUDIT-ROLLBACK-' + suffix)) == '0',
              'audit failure rolls back actual business row')
        check(mysql('SELECT COUNT(*) FROM audit_event;') == before, 'audit failure rolls back parent event as well')
        failed = call('PUT', '/system/tenant', admin, json=dict(tenant_body, status='1'))
        check(failed.get('code') != 200, 'tenant status request fails when audit persistence fails')
        check(mysql('SELECT status FROM sys_tenant WHERE tenant_id=' + quote(tenants[0])) == '0',
              'audit persistence failure rolls back tenant status')
        check(mysql('SELECT COUNT(*) FROM audit_event;') == before, 'failed tenant update leaves no partial audit event')
    finally:
        mysql('DROP TRIGGER qa_audit_fail;')
    check(call('POST', '/compliance/dataAudit', admin, json={}).get('code') != 200, 'no public audit write endpoint')
    check(call('DELETE', '/compliance/dataAudit/' + str(events[-1]['id']), admin).get('code') != 200,
          'no public audit delete endpoint')
    print('DATA_AUDIT_API: PASS (' + str(COUNT) + ' assertions)', flush=True)
    return sample


def main():
    global BASE, CLIENT
    server = None
    frontend = None
    logfiles = []
    password = secrets.token_urlsafe(18)
    work = ROOT / 'target'
    try:
        fixture_schema(password)
        database_command = ['docker', 'exec', '-i', CONTAINER, 'sh', '-c',
            'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql -uroot '
            '--default-character-set=utf8mb4 --batch --skip-column-names']
        subprocess.run([sys.executable, '-X', 'utf8', str(ROOT / 'tools/check_db_schema.py'),
            '--database', DATABASE, '--mysql-cmd', shlex.join(database_command),
            '--json', str(work / 'audit-db-schema.json')], cwd=ROOT, check=True)
        CLIENT = mysql("SELECT client_id FROM sys_client WHERE status='0' AND grant_type LIKE '%password%' LIMIT 1;")
        port = available_port()
        BASE = 'http://localhost:' + str(port)
        environment = os.environ.copy()
        settings = inspect(CONTAINER)['Config']['Env']
        database_password = next(value.split('=', 1)[1] for value in settings if value.startswith('MYSQL_ROOT_PASSWORD='))
        database_port = os.environ.get('AUDIT_TEST_MYSQL_PORT', '3308')
        for source, database in [('MASTER', DATABASE)]:
            prefix = 'SPRING_DATASOURCE_DYNAMIC_DATASOURCE_' + source + '_'
            environment[prefix + 'URL'] = 'jdbc:mysql://localhost:' + database_port + '/' + database + '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai'
            environment[prefix + 'PASSWORD'] = database_password
        redis_config = inspect('codex-report-local-redis')['Config']
        redis = redis_config['Cmd']
        redis_environment = dict(value.split('=', 1) for value in redis_config.get('Env', []) if '=' in value)
        environment['SPRING_DATA_REDIS_PASSWORD'] = redis_environment.get('REDIS_PASSWORD', '')
        if '--requirepass' in redis:
            environment['SPRING_DATA_REDIS_PASSWORD'] = redis[redis.index('--requirepass') + 1]
        environment['SPRING_DATA_REDIS_PORT'] = '6381'
        environment['SPRING_DATA_REDIS_DATABASE'] = '11'
        java = Path(os.environ['JAVA_HOME']) / 'bin/java.exe'
        logfile = (work / 'audit-api-server.log').open('w', encoding='utf-8')
        logfiles.append(logfile)
        jar = BACKEND / 'ruoyi-admin/target/ruoyi-admin-audit-verification.jar'
        assert jar.is_file(), 'build audit-verification artifact first'
        flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
        server = subprocess.Popen([str(java), '-jar', str(jar), '--spring.profiles.active=dev',
            '--server.port=' + str(port), '--api-decrypt.enabled=false', '--captcha.enable=false',
            '--snail-job.enabled=false', '--spring.boot.admin.client.enabled=false',
            '--logging.level.org.dromara=INFO'], cwd=BACKEND, env=environment,
            stdout=logfile, stderr=subprocess.STDOUT, creationflags=flags)
        for _ in range(160):
            assert server.poll() is None, 'isolated backend exited; inspect target/audit-api-server.log'
            try:
                if requests.get(BASE + '/auth/code', timeout=1).status_code == 200:
                    break
            except requests.RequestException:
                time.sleep(.5)
        else:
            raise AssertionError('isolated backend startup timeout')
        sample = run_api(password)
        if os.environ.get('AUDIT_TEST_UI') == '1':
            front_port = available_port()
            frontend_env = os.environ.copy()
            frontend_env.update({'VITE_SERVICE_BASE_URL': BASE, 'VITE_APP_ENCRYPT': 'N',
                                 'VITE_APP_CLIENT_ID': CLIENT, 'VITE_PROXY_LOG': 'N'})
            front_log = (work / 'audit-ui-server.log').open('w', encoding='utf-8')
            logfiles.append(front_log)
            frontend = subprocess.Popen([shutil.which('node'), 'node_modules/vite/bin/vite.js', '--mode', 'dev',
                '--host', 'localhost', '--port', str(front_port), '--strictPort'], cwd=ROOT / 'template-frontend',
                env=frontend_env, stdout=front_log, stderr=subprocess.STDOUT, creationflags=flags)
            ui_url = 'http://localhost:' + str(front_port)
            for _ in range(100):
                assert frontend.poll() is None, 'isolated frontend exited'
                try:
                    if requests.get(ui_url, timeout=1).status_code == 200:
                        break
                except requests.RequestException:
                    time.sleep(.3)
            ui_env = os.environ.copy()
            ui_env.update({'AUDIT_QA_URL': ui_url, 'AUDIT_QA_PASSWORD': password, 'AUDIT_QA_SAMPLE': sample})
            subprocess.run([shutil.which('node'), str(BACKEND / 'script/tests/data_audit_ui.cjs')],
                           cwd=ROOT, env=ui_env, check=True, timeout=160)
    finally:
        for process in [frontend, server]:
            if process is not None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
        for logfile in logfiles:
            logfile.close()
        assert DATABASE.startswith('codex_audit_test_') and re.fullmatch(r'\w+', DATABASE)
        mysql('DROP DATABASE IF EXISTS `' + DATABASE + '`;', 'mysql')


if __name__ == '__main__':
    try:
        main()
    except Exception:
        print('DATA_AUDIT_API_OR_UI: FAIL', flush=True)
        raise
