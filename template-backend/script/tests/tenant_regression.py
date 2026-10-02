"""Real MySQL + Spring Boot tenancy regression using a disposable database.

Run: python script/tests/tenant_regression.py
Requires local mysql-5.7 and ruoyi-redis-5x Docker containers, Python requests/bcrypt,
and a built ruoyi-admin.jar. Expected: PASS groups and TENANT_REGRESSION: PASS.
Boundary cases include missing/forged tenant, cross-tenant IDs and mixed bulk IDs.
No credentials are written to files. --hold keeps the fixture alive for browser QA.
"""
import argparse
import io
import json
import os
from pathlib import Path
import secrets
import subprocess
import time
import zipfile

import bcrypt
import requests

ROOT = Path(__file__).resolve().parents[2]
PORT = 18081
BASE = f'http://127.0.0.1:{PORT}'
DB = 'ry_tenant_test_' + secrets.token_hex(4)
checked = 0

def mysql(sql, database=None):
    command = 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 -N'
    if database:
        assert database.replace('_', '').isalnum()
        command += ' --database=' + database
    result = subprocess.run(['docker', 'exec', '-i', 'mysql-5.7', 'sh', '-c', command],
                            input=sql, text=True, encoding='utf-8', capture_output=True, check=True)
    return result.stdout.strip()

def check(condition, label):
    global checked
    assert condition, label
    checked += 1

def inspect(name):
    return json.loads(subprocess.check_output(['docker', 'inspect', name], text=True, encoding='utf-8'))[0]

def call(method, path, token=None, **kwargs):
    headers = {'clientid': CLIENT}
    if token: headers['Authorization'] = 'Bearer ' + token
    response = requests.request(method, BASE + path, headers=headers, timeout=20, **kwargs)
    if 'application/json' in response.headers.get('Content-Type', ''):
        return response.json()
    return response

def ok(result, label):
    check(result.get('code') == 200, f'{label}: code={result.get("code")}, msg={result.get("msg")}')
    return result.get('data')

def denied(result, label):
    check(result.get('code') != 200, label)

def login(name, password):
    return ok(call('POST', '/auth/login', json={'username': name, 'password': password,
        'clientId': CLIENT, 'grantType': 'password'}), 'login')['access_token']

def run(admin, password, role):
    suffix = secrets.token_hex(3)
    tenant_a, tenant_b = 'qa_a_' + suffix, 'qa_b_' + suffix
    for tid, name in [(tenant_a, '验收租户 A'), (tenant_b, '验收租户 B')]:
        ok(call('POST', '/system/tenant', admin, json={'tenantId': tid, 'tenantName': name, 'status': '0'}), 'create tenant')
    denied(call('POST', '/system/tenant', admin, json={'tenantId': tenant_a, 'tenantName': '重复编号', 'status': '0'}), 'duplicate tenant')
    denied(call('POST', '/system/tenant', admin, json={'tenantId': '__platform__', 'tenantName': '保留编号', 'status': '0'}), 'reserved platform log tenant')
    options = ok(call('GET', '/system/tenant/options', admin), 'tenant options')
    tenants = {row['tenantId']: row for row in options}
    print('PASS: tenant create, duplicate identifier and options', flush=True)

    user_a, user_b, user_a2 = ('qa_user_a_' + suffix, 'qa_user_b_' + suffix, 'qa_user_a2_' + suffix)
    def user_payload(name, tenant):
        return {'userName': name, 'nickName': name, 'tenantId': tenant, 'password': password,
                'roleIds': [role], 'status': '0', 'sex': '0'}
    denied(call('POST', '/system/user', admin, json=user_payload('qa_missing_' + suffix, None)), 'missing user tenant')
    denied(call('POST', '/system/user', admin, json=user_payload('qa_invalid_' + suffix, 'does_not_exist')), 'invalid user tenant')
    for name, tenant in [(user_a, tenant_a), (user_b, tenant_b), (user_a2, tenant_a)]:
        ok(call('POST', '/system/user', admin, json=user_payload(name, tenant)), 'create user')
    records = call('GET', '/system/user/list', admin, params={'userName': 'qa_user_', 'pageSize': 100})['rows']
    users = {row['userName']: row for row in records}
    a, b = login(user_a, password), login(user_b, password)
    own_options = ok(call('GET', '/system/tenant/options', a), 'own tenant options')
    check([row['tenantId'] for row in own_options] == [tenant_a], 'only own tenant option')
    denied(call('GET', '/system/tenant/list', a), 'tenant management admin only')
    denied(call('POST', '/system/user', a, json=user_payload('qa_forged_' + suffix, tenant_b)), 'cross-tenant user creation')
    edit_user = user_payload(user_a, tenant_b)
    edit_user['userId'] = users[user_a]['userId']
    denied(call('PUT', '/system/user', admin, json=edit_user), 'immutable user tenant')
    denied(call('GET', '/system/user/' + str(users[user_b]['userId']), a), 'cross-tenant user detail')
    print('PASS: mandatory user tenant, immutable ownership and user isolation', flush=True)

    prefix = 'tenant_qa_' + suffix
    def notice(title, **extra):
        return dict(noticeTitle=title, noticeType='1', noticeContent='真实 API 隔离验收', status='0', **extra)
    ok(call('POST', '/system/notice', a, json=notice(prefix + '_a', tenantId=tenant_b)), 'create A notice with forged tenant')
    ok(call('POST', '/system/notice', b, json=notice(prefix + '_b')), 'create B notice')
    rows = call('GET', '/system/notice/list', admin, params={'noticeTitle': prefix})['rows']
    check(len(rows) == 2, 'admin sees both tenants')
    notices = {row['noticeTitle']: row for row in rows}
    na, nb = notices[prefix + '_a'], notices[prefix + '_b']
    check(na['tenantId'] == tenant_a and nb['tenantId'] == tenant_b, 'server overrides forged tenant')
    for token, expected in [(a, na['noticeId']), (b, nb['noticeId'])]:
        rows = call('GET', '/system/notice/list', token, params={'noticeTitle': prefix})['rows']
        check([row['noticeId'] for row in rows] == [expected], 'tenant notice list scope')
    foreign = call('GET', '/system/notice/' + str(nb['noticeId']), a)
    check(foreign.get('code') != 200 or foreign.get('data') is None, 'cross-tenant notice detail')
    changed = notice(prefix + '_b', noticeId=nb['noticeId'], tenantId=tenant_a)
    denied(call('PUT', '/system/notice', a, json=changed), 'cross-tenant update')
    denied(call('DELETE', '/system/notice/' + str(nb['noticeId']), a), 'cross-tenant delete')
    own_change = notice(prefix + '_a', noticeId=na['noticeId'], tenantId=tenant_b)
    ok(call('PUT', '/system/notice', a, json=own_change), 'update own notice')
    check(ok(call('GET', '/system/notice/' + str(na['noticeId']), admin), 'admin detail')['tenantId'] == tenant_a, 'update cannot move ownership')
    print('PASS: notice list/detail/update/delete isolation, forged tenant rejected and admin sees all', flush=True)

    ids = f'{users[user_a2]["userId"]},{users[user_b]["userId"]}'
    denied(call('PUT', '/system/role/authUser/cancelAll', a, params={'roleId': role, 'userIds': ids}), 'mixed-tenant bulk cancel')
    ok(call('GET', '/system/user/getInfo', b), 'foreign session survives rejected bulk cancel')
    denied(call('GET', '/monitor/logininfor/unlock/' + user_b, a), 'cross-tenant account unlock')
    export = call('POST', '/system/user/export', a, data={'userName': 'qa_user_'})
    check(isinstance(export, requests.Response) and export.status_code == 200, 'user export response')
    with zipfile.ZipFile(io.BytesIO(export.content)) as workbook:
        xml = ''.join(workbook.read(name).decode('utf-8') for name in workbook.namelist() if name.endswith('.xml'))
        check(user_a in xml and user_b not in xml, 'export tenant isolation')
    print('PASS: mixed batch rejected, foreign session preserved, account unlock and export isolation', flush=True)

    for _ in range(30):
        logs = call('GET', '/monitor/logininfor/list', a, params={'pageSize': 100})
        if any(row.get('userName') == user_a for row in logs.get('rows', [])):
            break
        time.sleep(.3)
    check(any(row.get('userName') == user_a for row in logs.get('rows', [])), 'successful login log has own tenant')
    check(all(row.get('tenantId') == tenant_a for row in logs.get('rows', [])), 'login log scope')
    disabled = {key: tenants[tenant_b][key] for key in ['id', 'tenantId', 'tenantName', 'status']}
    disabled['status'] = '1'
    ok(call('PUT', '/system/tenant', admin, json=disabled), 'disable tenant')
    denied(call('GET', '/system/user/getInfo', b), 'disabled tenant active session')
    denied(call('POST', '/auth/login', json={'username': user_b, 'password': password, 'clientId': CLIENT, 'grantType': 'password'}), 'disabled tenant login')
    disabled['status'] = '0'
    ok(call('PUT', '/system/tenant', admin, json=disabled), 'enable tenant')
    denied(call('DELETE', '/system/tenant/' + str(tenants[tenant_a]['id']), admin), 'referenced tenant cannot delete')
    denied(call('DELETE', '/system/tenant/1', admin), 'default tenant cannot delete')
    print('PASS: login log tenancy, disabled tenant sessions/login and protected deletion', flush=True)

    routes = ok(call('GET', '/system/menu/getRouters', admin), 'admin menu')
    system = next(row for row in routes if row.get('path') == '/system')
    check(system['children'][0].get('path') in ('tenant', '/system/tenant'), 'tenant menu is first')
    print('PASS: tenant is first in System Management', flush=True)
    print(f'TENANT_REGRESSION: PASS ({checked} assertions)', flush=True)
    return user_a, tenant_a

def main():
    global CLIENT
    args = argparse.ArgumentParser()
    args.add_argument('--hold', action='store_true')
    options = args.parse_args()
    server = None
    logfile = None
    try:
        mysql('CREATE DATABASE `' + DB + '` CHARACTER SET utf8mb4;')
        tables = mysql("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='ry-vue' AND TABLE_TYPE='BASE TABLE';").splitlines()
        for table in tables:
            assert table.replace('_', '').isalnum()
            mysql(f'CREATE TABLE `{table}` LIKE `ry-vue`.`{table}`;', DB)
            if not table.startswith('sj_') and table not in ('sys_oper_log', 'sys_logininfor', 'sys_oss', 'sys_social'):
                mysql(f'INSERT INTO `{table}` SELECT * FROM `ry-vue`.`{table}`;', DB)
        migration = (ROOT / 'script/sql/update/update_5.6.2-tenant-version.sql').read_text(encoding='utf-8')
        mysql(migration, DB)
        mysql(migration, DB)
        check(int(mysql("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id';", DB)) >= 8, 'migration tenant columns')
        print('PASS: migration applied twice without losing source data', flush=True)
        password = secrets.token_urlsafe(12)
        hashed = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()
        mysql(f"UPDATE sys_user SET password='{hashed}' WHERE user_id=1;", DB)
        CLIENT = mysql("SELECT client_id FROM sys_client WHERE status='0' AND grant_type LIKE '%password%' LIMIT 1;", DB)
        role = int(mysql('SELECT COALESCE(MAX(role_id), 1) + 100 FROM sys_role;', DB))
        mysql(f"INSERT INTO sys_role(role_id,role_name,role_key,role_sort,status,del_flag,menu_check_strictly) VALUES({role},'租户验收角色','tenant-regression',100,'0','0',1); "
              f"INSERT INTO sys_role_menu(role_id,menu_id) SELECT {role},menu_id FROM sys_menu WHERE COALESCE(perms,'') <> 'system:tenant:list';", DB)
        environment = os.environ.copy()
        config = inspect('mysql-5.7')['Config']['Env']
        environment['SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD'] = next(value.split('=', 1)[1] for value in config if value.startswith('MYSQL_ROOT_PASSWORD='))
        environment['SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL'] = f'jdbc:mysql://localhost:3306/{DB}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
        redis = inspect('ruoyi-redis-5x')['Config']['Cmd']
        if '--requirepass' in redis:
            environment['SPRING_DATA_REDIS_PASSWORD'] = redis[redis.index('--requirepass') + 1]
        environment['SPRING_DATA_REDIS_PORT'] = '6380'
        environment['SPRING_DATA_REDIS_DATABASE'] = '15'
        java = os.environ.get('TENANT_TEST_JAVA', r'D:\soft\scoop\global\apps\openjdk17\current\bin\java.exe')
        (ROOT / 'target').mkdir(exist_ok=True)
        logfile = (ROOT / 'target/tenant-regression-server.log').open('w', encoding='utf-8')
        server = subprocess.Popen([java, '-jar', str(ROOT / 'ruoyi-admin/target/ruoyi-admin.jar'), f'--server.port={PORT}',
            '--api-decrypt.enabled=false', '--captcha.enable=false', '--snail-job.enabled=false', '--spring.boot.admin.client.enabled=false',
            '--logging.level.org.dromara.common.web.interceptor=OFF'], cwd=ROOT, env=environment, stdout=logfile, stderr=subprocess.STDOUT)
        for _ in range(100):
            if server.poll() is not None: raise RuntimeError('Test server exited; inspect target/tenant-regression-server.log')
            try:
                if requests.get(BASE + '/auth/code', timeout=1).status_code == 200: break
            except requests.RequestException:
                pass
            time.sleep(.5)
        else: raise RuntimeError('Test server startup timeout')
        admin = login('admin', password)
        qa_user, qa_tenant = run(admin, password, role)
        if options.hold:
            print('QA_ACCESS:' + json.dumps({'token': admin, 'client': CLIENT, 'database': DB, 'port': PORT, 'password': password, 'user': qa_user, 'tenant': qa_tenant}), flush=True)
            input()
    finally:
        if server:
            server.terminate()
            try: server.wait(timeout=15)
            except subprocess.TimeoutExpired: server.kill()
        if logfile: logfile.close()
        assert DB.startswith('ry_tenant_test_') and DB.replace('_', '').isalnum()
        mysql('DROP DATABASE IF EXISTS `' + DB + '`;')

if __name__ == '__main__':
    main()
