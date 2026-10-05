# 报告 JSON 到 DOCX 渲染器

`render_report_docx.py` 只负责读取一个 UTF-8 JSON 对象并渲染 DOCX 模板。数据库查询、癌种条件、客户默认值、基因排序和质控判断均由 Java 在生成 JSON 时完成。

脚本与依赖清单随仓交付，位置（Java 侧从 classpath 释放到临时目录，部署时无需配路径）：

```text
template-backend/ruoyi-modules/ruoyi-report/src/main/resources/scripts/
  ├── render_report_docx.py
  └── requirements-report-renderer.txt
template-backend/script/tests/test_render_report_docx.py    # 单测（含真实模板渲染用例）
```

## 模板文件放哪（固定目录约定）

模板文件**平铺**在一个固定目录里，文件名 = **模板名 + `.docx`**（模板名取 `report_template.template_name`）。

```text
/home/liushangzhi/project/templates/                          # 固定目录（容器内挂 /app/templates）
├── 同源重组修复（HRR）通路基因检测报告-圣域.docx                # 模板实体
└── 同源重组修复（HRR）通路基因检测报告-圣域.example.json        # 渲染入参样例（试渲染/联调用）
```

查找口径只有一条路：**报告的产品 → product_template → report_template → 固定目录/模板名.docx**。
产品没配模板就报「当前产品未配置模板：xxx」；目录里没有对应文件就报「模板文件不存在：<路径>」，
不做 classpath / 任意路径兜底。`template_path` 只作登记说明，不参与查找。

固定目录由启动参数给定：`--report.renderer.templates-root=/app/templates`
（环境变量写法 `REPORT_RENDERER_TEMPLATESROOT`；本机 deploy.sh 的 `TEMPLATES_DIR` 指向宿主目录）。
新增/改模板只要把文件丢进这个目录，不用重新打 jar。

Java 调用方：`org.dromara.report.service.ReportDocxRenderer`（ProcessBuilder 参数数组 + stdout/stderr
重定向到 `.renderer-*.log` + 超时后 `destroyForcibly`；退出码 2/3/4 与本文错误码逐字对应）。
配置项（默认值可直接跑，需要时用环境变量覆盖，与参考工程同名）：
`REPORT_RENDERER_PYTHON_COMMAND`（默认 `python3`）、`REPORT_RENDERER_SCRIPT`（默认用 classpath 里的脚本）、
`REPORT_RENDERER_TIMEOUT_SECONDS`（默认 60）。

> 提示：运行 JVM 的机器/容器必须装 `python3` + docxtpl。本机部署用
> `~/docker/biotech-plus/Dockerfile.backend`（`./deploy.sh build-image`），
> 参考工程的生产镜像同样是「temurin JRE + python3-pip + pip install」。

## 安装

```bash
# 本地调试用；随 jar 运行时脚本会自己从 classpath 释放，不需要这一步
python -m pip install -r ruoyi-modules/ruoyi-report/src/main/resources/scripts/requirements-report-renderer.txt
```

建议使用 Python 3.10 或更高版本，并为报告渲染服务创建独立虚拟环境。

## 生成报告

```bash
# 在 template-backend/ruoyi-modules/ruoyi-report 下执行（脚本路径按上文）
python src/main/resources/scripts/render_report_docx.py \
  "src/main/resources/report-templates/同源重组修复（HRR）通路基因检测报告-圣域_v1.docx" \
  "src/main/resources/report-templates/同源重组修复（HRR）通路基因检测报告-圣域_v1.example.json" \
  /tmp/report.docx
```

成功时进程退出码为 `0`，标准输出为一行 JSON，`status` 等于 `GENERATED`。脚本先在内存中完成渲染和 DOCX 完整性检查，再通过同目录临时文件原子替换输出文件。

## 上线前校验

```bash
python scripts/render_report_docx.py \
  src/main/resources/report-templates/pharma-shengyu/v1/template.docx \
  input.json \
  --validate-only
```

校验会实际渲染到内存，但不会生成文件。缺少字段、非法 Jinja 语法、非对象 JSON 根节点或残留模板标签都会返回非零退出码，并在标准错误输出一行结构化错误 JSON。

## Java 调用约定

Java 依次传入模板路径、JSON 文件路径和输出 DOCX 路径，等待进程结束后读取标准输出、标准错误和退出码。退出码含义：

- `0`：校验或生成成功；
- `2`：输入路径或 JSON 错误；
- `3`：模板语法、缺少字段或渲染错误；
- `4`：输出文件写入错误。

同一模板可以被并发调用。脚本始终从模板字节的内存副本创建 `DocxTemplate`，不会修改原模板。
