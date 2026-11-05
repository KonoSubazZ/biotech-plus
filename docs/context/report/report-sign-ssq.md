# 上上签电子签署送签通道（三个场景）交付说明

> 对应实现：`ruoyi-report` 模块的 `ISsqSignService` / `SsqSignServiceImpl` / `SsqSignProperties`。
> 参考实现：`/home/liushangzhi/project/ssq_api.py`（诺禾 iPaaS → 上上签，UAT 已跑通）。
> 本文凡未取得真实值处一律写 `<联调填入>` 或 `<待确认>`，**不编造任何响应样例或生产域名**。

---

## 1. 网关链路

```
POST {base-url}/api/oauth/oauth/token                       ← client_credentials 取 token（按 expires_in 缓存）
POST {base-url}/api/hitf/v2p/rest/invoke/<Base64>           ← 送签 / 关键字定位
GET  {base-url}/api/hitf/v2p/rest/invoke/best-signquery-contract-info/{contractId}   ← 查签署状态（本次未接，见 §6）
```

- 网关地址（UAT）：`https://ipaas-uat.novogene.com`；`client_id`：`onco-pharma-platform`。
- invoke 端点末段 = `Base64("HZERO:BESTSIGN:" + 接口名)`（实测解码确认），代码里用 `Base64.getEncoder()` 现算，不抄魔法串：
  - `SendContractsSync` → `SFpFUk86QkVTVFNJR046U2VuZENvbnRyYWN0c1N5bmM=`
  - `LocateByMultiKeywords`、`DownloadFile` 同前缀 `HZERO:BESTSIGN:`。
- 主链路：Token → `LocateByMultiKeywords`（关键字拿坐标）→ 组装签署单（roles + appendingSignLabels）→ `SendContractsSync`（送签，返回合同号）。
- Token 处理：`401` 或响应 `code=error.permission.accessTokenInvalid` 均判为失效，**强制换一次新 token** 后重试一次（以网关为准：本地缓存按 `expires_in` 提前 60 秒估算，网关提前吊销或时钟偏差时缓存可能还没到点，所以失效重试必须绕过缓存）；平时按 `expires_in` 提前 60 秒换。

---

## 2. 三个场景对照表

来源：`ssq_api.py`（`send_report` / `send_delay` / `send_qc`）。

| 项 | 报告交付（`send_report`） | 报告延迟（`send_delay`） | 质控不合格（`send_qc`） |
|---|---|---|---|
| 定位关键字 | `报告解读人：`、`报告复核人：`、`审批人：`（全角冒号） | `反馈人`、`复核人`、`反馈时间`、`复核时间` | 同报告延迟 |
| roles[1] | 报告解读人 / PERSON / needNotice=true / routeOrder=1 | 反馈人 / PERSON / needNotice=true / 1 | 反馈人 / PERSON / needNotice=true / 1 |
| roles[2] | 报告复核人 / PERSON / true / 2 | 复核人 / PERSON / true / 2 | 复核人 / PERSON / true / 2 |
| roles[3] | 审批人 / PERSON / true / 3 | 本企业 / ENTERPRISE / needNotice=false + verifyMethod=NONE / 3 | 本企业 / ENTERPRISE / **signMode=AUTO + autoSign=true** + needNotice=false + verifyMethod=NONE / 3 |
| roles[4] | 本企业 / ENTERPRISE / `{needNotice:false}`（**无 verifyMethod**） / 4 | — | — |
| 企业章 SEAL | 压在 `审批人：` 坐标 **x-0.1, y-0.1** | 压在 `反馈时间` 坐标 **x+0.1, y-0.1** | 同报告延迟 |
| 反馈人/解读人 SIGNATURE | `报告解读人：` 坐标 **+0, +0** | `反馈人` 坐标 **+0.125, -0.025** | `反馈人` 坐标 **+0.145, -0.025** |
| 解读人 DATE | `报告解读人：` 坐标 **+0.035, -0.03**，`yyyy-MM-dd` | `反馈时间` 坐标 **+0.17, -0.01** | 同报告延迟 |
| 复核人 SIGNATURE | `报告复核人：` 坐标 **+0, +0** | `复核人` 坐标 **+0.125, -0.025** | `复核人` 坐标 **+0.145, -0.025** |
| 复核人 DATE | `报告复核人：` 坐标 **+0.035, -0.03** | `复核时间` 坐标 **+0.17, -0.01** | 同报告延迟 |
| 审批人 SIGNATURE/DATE | `审批人：` 坐标 +0,+0 / +0.035,-0.03 | — | — |
| 共用字段 | `templateId=4197207713522448386`、`signOrderly="true"`（**字符串**）、`sendAction="SEND"`、documents[0].order="0"、`contractConfig.contractTitle=fileName`、`content=base64(PDF 全文)` | 同左 | 同左 |

**坐标口径：** 关键字坐标是运行时从网关 `LocateByMultiKeywords` 取的（PDF 版式不同坐标不同，取不到即报错，不兜底）；
上表相对坐标的**偏移量是调试出来的固定值**，写死在三个 payload 方法里（不开放为配置项），改版式时只改这一处。

---

## 3. 配置项与生产切换

`ssq.*`（`SsqSignProperties`，前缀 `ssq`）：

| key | 含义 | 本机/测试 | 生产 |
|---|---|---|---|
| `base-url` | iPaaS 网关地址 | `https://ipaas-uat.novogene.com` | `<待确认>` |
| `client-id` | OAuth client_id | `onco-pharma-platform` | `<待确认>` |
| `client-secret` | OAuth client_secret | 见 `application-dev.yml` | `<待确认>` |
| `template-id` | 签署模板ID（三场景共用） | `4197207713522448386` | `<待确认>`（模板建在环境里，与 UAT 大概率不同） |
| `timeout-seconds` | 单次 HTTP 超时（秒） | `30` | `30` |

**本机网络前提（2026-10-13 实测，必须先满足才能联调）：**

`ipaas-uat.novogene.com` 解析到内网 **172.17.20.31**，而本机 docker0 的网段就是 `172.17.0.0/16`
（`172.17.0.0/16 dev docker0 proto kernel`）—— 宿主机路由把发往该地址的包全送进 docker0，
于是宿主机 curl 与容器都报 `No route to host`。**这是路由冲突，不是凭据或代码问题**：

```bash
# 实测：绑定 eno4 直连完全通（ping 通；根路径 200；token 接口返回 access_token, expires_in=3599）
ping -I eno4 172.17.20.31
curl --interface eno4 -sS -o /dev/null -w "%{http_code}\n" https://ipaas-uat.novogene.com/
```

修法与既有先例一致（路由表里 `172.17.27.105 via 172.20.1.254 dev eno4` 就是为绕开同一个冲突加的 /32）：

```bash
# 需要 root，在本机执行；重启后失效（netplan 里没有这条静态路由的定义，说明历来是手工加的）
sudo ip route add 172.17.20.31 via 172.20.1.254 dev eno4
```

注意：**后端起容器是 `--network host`，同样走宿主路由** —— 不加这条路由，本机跑起来的报告系统也调不通上上签。

**配置口径（profile 一句话对齐）：**

- `application-dev.yml` = 本机 / 测试（UAT 域名 + 测试账号 + 测试 templateId）——本次已写入。
- `application-prod.yml` = 生产（域名 / client_secret / templateId **待对接人确认**，本次**留空**）——本次已写入空块。
- `application.yml`（公共文件）**不放 `ssq` 块**，避免「一个 key 两处值」口径打架。
- 值外层套 `${SSQ_*:}`：可被运行时环境变量覆盖（`SSQ_BASE_URL` / `SSQ_CLIENT_ID` / `SSQ_CLIENT_SECRET` / `SSQ_TEMPLATE_ID`，Spring relaxed binding），生产上线时可在部署机 `export` 顶掉而不改文件。
- 生产留空 = 调用时快速失败（报「未配置 ssq.base-url」），不会静默连到测试环境。

**部署 profile：** 本机 `deploy.sh` 的 `deploy` 段与 `restart-backend` 段两处 `SPRING_PROFILES_ACTIVE` 已改为 `dev`
（原为 `prod`）——本机容器现在读 `application-dev.yml`（本机 = 测试配置）。**生产机上那份脚本保持 `prod`。**
备份：`/home/liushangzhi/docker/biotech-plus/deploy.sh.bak-20261013`（改动前原样，`prod` 两处）。

**签名人写死位置：** 本阶段签名用户写死在 `SsqSignServiceImpl` 的 `private static final` 常量，**不进配置**；
接业务数据（解读人 / 复核人 / 审批人）时再放开为入参。

| 常量 | 值 | 说明 |
|---|---|---|
| `SIGNER_ACCOUNT` / `SIGNER_NAME` | `18034334041` / `刘尚智` | **个人角色**（报告解读人 / 报告复核人 / 审批人 / 反馈人 / 复核人）——2026-10-13 按用户口径替换（文档示例原为 `18610464338` / 罗阔） |
| `ENTERPRISE_OPERATOR_ACCOUNT` | `18610464338` | **企业盖章角色的操作账号**（文档示例值，本次不动；该角色 `needNotice=false`，不会给对方发通知） |
| `ENTERPRISE_NAME` | `天津诺禾医学检验所有限公司` | 企业签署主体 |

**环境变量与文件对应关系：** 同一份 jar 靠 `SPRING_PROFILES_ACTIVE`（运行时优先级高于打包填充的 `@profiles.active@`）
或 `SSQ_*` 环境变量切换环境，不改代码、不重打包。

---

## 4. 联调记录

> 首次联调完成后回填以下各项，未联调前保持占位。
>
> **2026-10-13 首次联调：已完成（报告交付 + 质控不合格两个场景；报告延迟按口径禁用）**
>
> - 前置：本机需要 `/32` 路由（见 §3「本机网络前提」），加完后宿主机 curl `https://ipaas-uat.novogene.com/` 返回 200。
> - 首次尝试失败过一次：`Connection reset / No route to host`（路由冲突），不是代码或凭据问题。
> - 容器跑测试时必须 `--network host`：普通 bridge 容器把 `172.17.20.31` 当同网段直连，绕不过 docker0。
> - 冒烟结果 `Tests run: 3, Failures: 0, Errors: 0, Skipped: 1`；两次送签耗时 3330 ms / 1993 ms，token 有效期约 1478 秒。
> - ⚠️ 真送签产生了 **2 份真实合同**并给对方手机号发了签署通知（`needNotice=true`）；联调后请到上上签后台作废测试合同。

| 项 | 值 |
|---|---|
| `SendContractsSync` 响应里合同号字段名 | **`data.contractId`**（字符串）—— 2026-10-13 UAT 实测，代码已把兼容解析收敛成直接取该字段 |
| UAT 真实响应样例 | `SendContractsSync` → `{"code":"0","message":"请求成功","data":{"contractId":"4220441872676125698"}}`；<br>`LocateByMultiKeywords` → `<未抓取>`（抓它要再走一次真送签、会再产生合同，本次不做） |
| 报告交付实测合同号 | `4220441872676125698`（文件：`TKHS260125414-…组织样本基因检测报告5-…34616085.pdf`，耗时 3330 ms） |
| 报告延迟实测合同号 | `<未跑>`（2026-10-13 口径：只跑报告交付 + 质控不合格；用例 `@Disabled`） |
| 质控不合格实测合同号 | `4220441900283034633`（文件：`qc-failed-notify-S2026007-20261012181208.pdf`，耗时 1993 ms） |

---

## 5. 与 `IReportSignService` 的关系

- `IReportSignService`（`ruoyi-report/.../service/IReportSignService.java`，含 `submit(reportId)` + `handleCallback`）
  是「**报告实体维度**」的签名流程：绑 `reportId`、写 `sign_flow_no`、接回调、下载签名件。
  **本阶段只有接口、无实现、无调用方**（预留）。
- 本次的 `ISsqSignService` 是「**文件维度**」的送签通道：入参 = 送签 PDF 路径，出参 = 合同号，
  三个场景各一个方法，参数由调用方给。
- 两者不冲突：将来 `IReportSignService` 的实现内部注入本次的 `ISsqSignService` 即可复用送签能力。
- 库列 `sign_flow_no` / `sign_status` / `report_signed_file_path` 已就位，供后续 `IReportSignService` 回填。

---

## 6. 本次不做（明确边界）

- 下载签名后 PDF（网关 `DownloadFile`）；
- 落盘到 `signed/`（报告：`<root>/report/signed/<analysis_date>/`，日期取 `analysis_report.analysis_date`；通知：`<root>/notify/signed/<yyyyMMdd>/`）；
- 签署状态查询（`best-signquery-contract-info/{contractId}`）与回调入口；
- `IReportSignService` 的实现（`submit(reportId)` + `handleCallback`）与库列回填（`sign_flow_no` / `sign_status` / `report_signed_file_path`）；
- Controller / 前端按钮（本次只交付 IService）；
- 签名人从业务数据取（解读人 / 复核人 / 审批人 → 用户账号映射）；
- 幂等（同一文件重复调用会产生多份合同，调用方需按业务状态拦住重复送签）、重试队列、异步化。

---

## 7. 未确认项（待对接人 / 待联调）

1. ~~`SendContractsSync` 响应里合同号字段名~~ —— **已确认 `data.contractId`**（2026-10-13 实测，见 §4）。
   实现里保留了「上上签送签原始响应（场景=…）」info 日志，线上排查时 `docker logs | grep 原始响应` 可回溯。
2. 生产网关域名 / 生产 `client_id` + `client_secret` / 生产 `templateId` —— 待对接人确认后填 `application-prod.yml`。
3. 「报告延迟」本地无样张（模板/样张只在远端 `/TJPROJ2/OBD/system/data/novo_cancer/utils/`）——
   延迟与质控关键字完全相同，联调可先用质控通知 PDF 代替。
4. 坐标偏移与模板版式强绑定：偏移量来自文档示例，模板换版式需重校（集中写在三个 payload 方法里）。
5. **本机 / 测试机访问 UAT 需要 /32 路由**（`172.17.20.31` 被 docker0 网段抢走，见 §3）：不满足时任何联调都会
   `No route to host`；本机后端（`--network host`）与 docker 里的测试容器都受它影响。
6. `application-prod.yml` 的 `ssq.*` 绑定目前只有「YAML 结构校验 + 应用以 dev profile 正常启动」这一级证据
   （Actuator `/actuator/env` 需鉴权，读不到）；等有调用方（Controller / `IReportSignService` 实现）注入后再补运行时证据。

---

## 8. 质控不合格通知链路（生信 / 湿实验，2026-10-13 新增）

生信质控与湿实验质控**共用同一份通知模板、同一组关键字、同一个「处理」弹窗**（靠 `qc_category` 区分取哪张质控表），
所以两者对接的是**同一个签名接口**（`ISsqSignService.sendQcUnqualified`），不是两套。

### 8.1 链路（页面动作 → 后端 → 文件 → 库）

```text
① 生成通知    页面对不合格样本点「处理」→ 填表 → 生成通知
              docxtpl 渲染 DOCX → <root>/notify/docx/<yyyyMMdd>/
              OnlyOffice 转 PDF → <root>/notify/pdf/<yyyyMMdd>/（失败只 warn，pdf_path 留空）
              落 qc_failed_notify（json_path / raw_docx_path / pdf_path），状态 = 已生成（文档）
② 送签        弹窗点「送签」→ POST /qc/notify/{id}/sign
              调 sendQcUnqualified(pdf_path) → 拿合同号
              写 sign_contract_id，状态 = 已送签（同时回写 bioinfo_qc / wet_lab_qc.notify_status）
③ 签署        被指定的签署人在上上签里签（异步，各自签完）
④ 拉签名件    弹窗点「刷新签署状态」→ POST /qc/notify/{id}/refresh-sign
              查 best-signquery-contract-info/{contractId} → 判完成
              → DownloadFile 下载 → <root>/notify/signed/<yyyyMMdd>/<与 PDF 同基名>.pdf
              → 写 signed_pdf_path，状态 = 已签署
⑤ 下载        弹窗点「下载签名件」→ GET /qc/notify/{id}/signed-pdf
```

状态流转：`已生成（文档）` → `已送签` → `已签署`（另有预留的 `已发送（邮件）`）。
权限点位：`qc:notify:query`（查/下载）、`qc:notify:generate`（生成）、`qc:notify:sign`（送签/刷新）。

### 8.2 签署人（按角色顺序固定，2026-10-13 口径）

| `routeOrder` | 角色（质控通知里） | userAccount | userName |
|---|---|---|---|
| `1` | 反馈人 | `18034334041` | `刘尚智` |
| `2` | 复核人 | `18610464338` | `罗阔` |
| 企业盖章角色 | 本企业 | `18610464338` | 不传 `userName`（只传 `enterpriseName`） |

同一张表也用于报告交付（解读人/复核人/审批人 = 1/2/3），改表位置见 `SsqSignServiceImpl.PERSON_SIGNERS`。
后期接业务数据时把这张表换成「按业务记录取账号」即可（本期不做）。

### 8.3 送签前置与防重

- `pdf_path` 为空（OnlyOffice 转换失败）→ 报错提示重新生成，**不隐式现场转 PDF**；
- 已有 `sign_contract_id` 的记录再点送签 → 直接报错（防重复合同）；要重签必须先重新生成通知（= 新增一条流水）。

### 8.4 实测结论（2026-10-13 全部收敛完毕）

**① 查询接口响应已抓全**（`GET /api/hitf/v2p/rest/invoke/best-signquery-contract-info/{contractId}`，合同 4220532292047296513）：

| 字段 | 值 | 说明 |
|---|---|---|
| `data.status` | `SENT` → `COMPLETE` | 合同状态：**`SENT` = 已派发待签，`COMPLETE` = 签署完成**（代码已按精确匹配收敛） |
| `data.contractId` / `contractTitle` / `templateId` | 4220532292047296513 / qc-failed-notify-S2026007-…pdf / 4197207713522448386 | 回执字段 |
| `data.sendTime` / `signDeadline` / `finishTime` | epoch 毫秒 | 派发时间 / 签署截止 / 完成时间 |
| `data.signers[]` | `roleName` + `participantName` + `routeOrder` + `status` | 逐个签署人状态：反馈人 刘尚智(1) / 复核人 罗阔(2) / 本企业 天津诺禾医学检验所有限公司(3)，三个都 COMPLETE |
| `data.sender.name` | 北京诺禾致源科技股份有限公司 | 发送方 |
| `data.entInternalDetails[].holders[]` | 天津诺禾医学检验所有限公司：盖章持有人 罗阔 / 18610464338 | 印证企业角色用 `18610464338` 是对的 |

**② `DownloadFile` 返回单份 PDF**（不是 zip）：`POST .../invoke/<Base64(DownloadFile)>`，body `{operator:{account:"",enterpriseName:""},contractIds:[…],fileType:"pdf"}` → 直接是 PDF 字节。
实测落盘 `reports/notify/signed/20261013/qc-failed-notify-S2026007-20261012185530.pdf`（317,159 字节，`%PDF-1.7` 头，
同记录转化 PDF 是 110,057 字节 —— 变大约 3 倍，即嵌入签名/盖章后的成品）；权限 644（`ArtifactPermissions.relax` 生效）。

**③ 端到端已实测（记录 id=41 / 湿实验 S2026007）**：
`POST /qc/notify/41/sign` → `contractId=4220532292047296513`、状态 `已送签`；
对方（刘尚智 + 罗阔）签完后 `POST /qc/notify/41/refresh-sign` → `contractStatus=COMPLETE`、`signed=true`、
`signedPdfPath=/app/reports/notify/signed/20261013/qc-failed-notify-S2026007-20261012185530.pdf`、状态 `已签署`；
库里 `qc_failed_notify.sign_contract_id` / `signed_pdf_path` / `notify_status` 与 `wet_lab_qc.notify_status` 全部写对。

**④ 签署是异步的**（反馈人、复核人各签一次），等待期间停在「已送签」是正常状态。
**拉取姿态已定稿（2026-10-13 用户决定）：只做手动按钮「刷新签署状态」，本期不做定时轮询**；
理由留痕：按钮成本已付出、联调期人工复现更直接；将来若上上签开放签署完成回调（webhook），回调或轮询都可作为自动化备选。
