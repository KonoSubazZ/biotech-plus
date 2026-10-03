# nkb 数据库（未来使用）

## 来源

`docs/context/nkb_20260202.sql` —— 上游 MySQL **5.7.26** 导出的全库 dump（79MB）。

> ⚠️ 该 dump **不入 git**（单文件 76MB，超过 GitHub 50MB 的建议上限，已在根 `.gitignore` 忽略）：
> 新克隆的仓库里**没有**这个文件，导入前要把它从服务器/共享盘放回 `docs/context/nkb_20260202.sql`。

```
Server version  5.7.26-log
内容            66 张表 + 51 个视图（无存储过程 / 触发器）
字符集          utf8 / utf8_general_ci（沿用原库，未改动）
导出时间        2026-02-02 14:42:58
```

## 导入

推荐用部署脚本（幂等，可重复跑）：

```bash
bash ~/docker/biotech-plus/deploy.sh nkb
```

手动等价命令：

```bash
docker exec biotech-plus-mysql mysql -uroot -proot -e \
  "CREATE DATABASE IF NOT EXISTS nkb DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;"
docker exec -i biotech-plus-mysql mysql -uroot -proot --default-character-set=utf8 nkb \
  < docs/context/nkb_20260202.sql
```

实测：5.7.26 上导入耗时约 9 秒，零报错。

## 注意事项

1. **环境版本与上游对齐 5.7.26。** biotech-plus 的 MySQL 已经从 8.0 降到 5.7.26
   （见 `script/docker/docker-compose.yml` 与 `~/docker/biotech-plus/deploy.sh`）。
   注意 JDBC 连接串里的 `useSSL=false` —— 5.7.26 的 TLS 版本太老（TLS 1.3 要 5.7.28+），
   8.0 驱动开着 SSL 会握手失败。
2. **dump 里的零日期**（`DEFAULT '0000-00-00 00:00:00'`）不需要额外处理：
   dump 自己带了 `SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO'`，直接用 `mysql < dump` 即可。
3. **DEFINER 是上游账号**（`novo@%` / `novo@localhost` / `root@localhost`），当前库里没有这些用户。
   dump 中 DEFINER 只出现在 51 个视图上，5.7 建视图不校验 DEFINER 用户，能正常创建和查询。
4. **不要把它放进 `deploy.sh` 的 init 目录**：79MB 会让每次「从零重建」都多跑一遍。
   已经是独立子命令 `./deploy.sh nkb`。
5. **文件不入库**：根 `.gitignore` 已忽略 `docs/context/nkb_20260202.sql`（及重复份
   `docs/context/nkb/nkb_20260202.sql`），`git rm --cached` 后本地文件保留、`./deploy.sh nkb` 照常可用。
   注意：**历史里仍留有早期提交带进来的同一份 blob**（远程仓库体积不会因此变小），
   要彻底瘦身得重写历史 + 强推，另议。将来更大的数据文件建议用 git-lfs 或私有存储。
