-- 5.6.5 · 圣域（pharma-shengyu）模板配置对齐参考工程
--
-- 为什么：本仓脚手架种子把这条模板写成了「圣域 1238 报告 / SOMATIC / customer_code=NULL / sha 为空」，
--         与参考工程（172.20.1.34:8806 biotech 库 report_template 唯一那行）对不上，
--         报告预览/生成取到的模板名与输出范围都不是圣域 HRR 的口径。
-- 口径来源：biotech.report_template 的 template_name / customer_code / report_type / module_code / template_sha256。
-- 模板实体：template-backend/ruoyi-modules/ruoyi-report/src/main/resources/report-templates/pharma-shengyu/v1/template.docx
--          sha256 = 2518E0A35427833FB9A26570129C215BAEAFF57A5644A2909A4065F45B5E396D（与参考工程登记值一致）
-- 报告命名（report_name）：圣域口径是「留空 = 用默认拼接规则
-- {{subbarcode}}{{client}}{{template_name}}{{report_id}}」，所以显式置空，
-- 避免历史误配一个命名模板；要改命名规则走「报告模板配置」页（保存时按变量白名单校验）。
-- 模板文件：固定目录 <模板目录>/同源重组修复（HRR）通路基因检测报告-圣域.docx（模板名 + .docx）。
-- 运行时按「报告产品 → product_template → report_template.template_name」定位文件，
-- template_path 只作登记说明，不参与查找（内容与文件名保持一致，便于人工核对）。
-- （模板目录见 docs/context/report/report-docx-renderer.md 与 deploy.sh 的 TEMPLATES_DIR）
-- 幂等：重复执行结果相同。
SET NAMES utf8mb4;

UPDATE `report_template`
   SET `template_name`   = '同源重组修复（HRR）通路基因检测报告-圣域',
       `customer_code`   = '圣域',
       `report_type`     = 'HRR',
       `module_code`     = 'SHENGYU_SOMATIC_VARIANTS_V1;SHENGYU_GERMLINE_VARIANTS_V1;SHENGYU_QC_V1',
       `template_path`   = '同源重组修复（HRR）通路基因检测报告-圣域.docx',
       `template_sha256` = '2518E0A35427833FB9A26570129C215BAEAFF57A5644A2909A4065F45B5E396D',
       `report_name`     = NULL,
       `update_time`     = NOW()
 WHERE `template_code` = 'pharma-shengyu'
   AND `del_flag` = '0';
