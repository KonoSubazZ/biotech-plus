# Canal CDC 数据变更日志 · 运维手册

> 目标：把 MySQL 业务表的 INSERT / UPDATE / DELETE 以「一行一条 JSON」写进独立日志文件。
> 不建表、不写库。实现在 `template-backend/ruoyi-modules/ruoyi-audit`。

## 1. 组成与位置

| 组件 | 说明 | 位置 |
|---|---|---|
| canal-server | 读 binlog，tcp 模式，host 网络 | 容器 `biotech-plus-canal`，端口 11111，配置/日志 `/home/liushangzhi/docker/biotech-plus/canal/logs` |
| 消费端 | 后端 `ruoyi-audit` 模块（`CanalCdcConsumer`），开关 `AUDIT_CDC_ENABLED=true` | 容器 `biotech-plus-backend` |
| 日志文件 | 一行一条 JSON，按天 + 200MB 滚动，保留 90 天 / 总量 20GB | 宿主 `/home/liushangzhi/project/logs/cdc/cdc-change.log`（容器内 `/app/logs/cdc/`） |
| binlog | ROW 格式，保留 7 天，单文件 512MB | MySQL 数据目录 `mysql-bin.*` |

## 2. 启动顺序（换机 / 重建时）

```bash
cd /home/liushangzhi/docker/biotech-plus
bash deploy.sh mysql-binlog    # 只重建 MySQL 容器打开 binlog（先做 mysqldump 冷备）
bash deploy.sh canal-up        # 起 canal-server
bash deploy.sh restart-backend # 起后端（带 AUDIT_CDC_ENABLED=true）
```

验证：
```bash
docker exec biotech-plus-mysql mysql -uroot -proot -N -e "SHOW VARIABLES LIKE 'log_bin'; SHOW MASTER STATUS;"
docker logs --tail 20 biotech-plus-canal | tail -5
docker logs --tail 100 biotech-plus-backend | grep -E "已连接 canal-server|CDC 变更日志已启动"
```

## 3. 看日志

```bash
L=/home/liushangzhi/project/logs/cdc/cdc-change.log

tail -f "$L"                                  # 实时
grep '"table":"sample_file"' "$L" | tail -50  # 某张表
grep '"pk":{"id":"35"}' "$L"                  # 某条记录
grep '"actorId":5' "$L"                       # 某个用户
grep '"op":"DELETE"' "$L"                     # 只看删除
grep -o '"binlog":"[^"]*"' "$L" | sort | uniq -d   # 重投产生的重复位点（为空即无重复）
```

一行字段：`time / db / table / op / pk / changes（UPDATE）/ data（INSERT、DELETE）/ actorId / actorName / actorType / binlog`。

## 4. 已知限制（不是 bug）

1. **一行 = 一次行变更**，不是一次业务动作；一次操作改 3 张表 5 行就是 5 行日志（不做事务聚合）。
2. **归因**：INSERT/DELETE 取行内 `create_by/update_by`；UPDATE 只有本次写了 `update_time` 才认 `update_by`。
   绕过应用的直写、或用 Wrapper 只改业务列的接口更新 → `actorType=SERVICE`（宁可记未知，不冒充上一个人）。
3. **至少一次投递**：重连/重投可能出现重复行，按 `binlog` 位点去重。
4. **不含 `file_*` 变体明细大表**（一次驱动几千行）；要放开就改 `deploy.sh` 的 filter 与 `audit-cdc.yml`，不改代码。
5. **canal-server 重启**可能从最新位点续读，停机窗口内的变更可能缺失。发现缺口的判据：日志文件时间序与 `SHOW MASTER STATUS` 位置明显脱节。需要补齐时，给 canal instance 配 `canal.instance.master.timestamp=<起始毫秒>` 重放。
6. `password/salt/token/secret/access_key/private_key/client_secret` 写 `***`；单列超 4000 字符截断并标注 `[truncated]`。

## 5. 回滚

```bash
docker stop biotech-plus-canal
# 去掉 deploy.sh 里两处 -e AUDIT_CDC_ENABLED=true，然后：
bash deploy.sh restart-backend
```

日志文件保留即可；彻底回退再撤掉 `ruoyi-audit` 模块与两处 pom 依赖。

## 6. 演示账号

MySQL root 账号写 `ry-vue`；canal 只读账号 `canal/canal_2026`（SELECT + REPLICATION SLAVE/CLIENT），
建号脚本：`template-backend/script/sql/update/update_5.6.30-canal-user.sql`。
