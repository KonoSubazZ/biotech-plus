-- 产品关联基因表（product_gene）+ 对应按钮权限
-- 依据：产品配置需要关联 nkb.ncbi_gene；每个产品一份 gene list，先建产品、后加基因
-- 规范：ai-rules/04-db-schema.md     体检：python3 tools/check_db_schema.py
--
-- 设计要点（实测得出）：
--   1. 冗余一份 gene_symbol：nkb 是**另一个库**，详情列表不该依赖跨库 join；
--      nkb 将来换 dump / 升级也不影响已关联的数据。
--   2. 唯一键用 (tenant_id, product_id, gene_id) 而**不是 gene_symbol** ——
--      实测 ncbi_gene 里 symbol 会重复（如 TRNAN-GUU 对应 19 个 gene_id，共 121 个重复 symbol），
--      gene_id 才是稳定的业务键。
--   3. gene_id 用 int unsigned，与源表 ncbi_gene.gene_id 对齐（源表 min=1 / max=1000000020）。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `product_gene` (
    id           bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',

    product_id   bigint       NOT NULL                          COMMENT '产品配置 id（product_config.id）',
    gene_id      int unsigned NOT NULL                          COMMENT '基因 id（nkb.ncbi_gene.gene_id）',
    gene_symbol  varchar(24)  NOT NULL                          COMMENT '基因符号（冗余自 nkb.ncbi_gene，展示时不必跨库 join）',
    remark       varchar(500)                                   COMMENT '备注',

    create_by    bigint                                         COMMENT '创建者',
    create_time  datetime                                       COMMENT '创建时间',
    update_by    bigint                                         COMMENT '更新者',
    update_time  datetime                                       COMMENT '更新时间',
    del_flag     char(1)      NOT NULL DEFAULT '0'              COMMENT '删除标志（0代表存在 1代表删除）',

    tenant_id    varchar(20)  NOT NULL DEFAULT '000000'         COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    -- 主查询是「某产品下的基因列表」，product_id 走这个索引
    KEY idx_product_gene_product_id (product_id),
    -- 防重复关联；用 gene_id 不用 symbol（symbol 会重复）
    UNIQUE KEY uk_product_gene (tenant_id, product_id, gene_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产品关联基因';


-- ============================================================================
-- 按钮权限（F 类型，挂在「产品配置」菜单下，不进侧边栏）
--   perms 与 ProductGeneController 的 @SaCheckPermission 逐字一致
-- ============================================================================
SET @menu_id = (SELECT menu_id FROM sys_menu WHERE path = 'product-config' AND menu_type = 'C' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '基因查询', @menu_id, 10, '', NULL, 1, 0, 'F', '0', '0', 'project:productGene:query', '#', 1, NOW(), '查询产品关联基因'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:productGene:query') AS c1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '基因新增', @menu_id, 11, '', NULL, 1, 0, 'F', '0', '0', 'project:productGene:add', '#', 1, NOW(), '添加产品关联基因'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:productGene:add') AS c2);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '基因删除', @menu_id, 12, '', NULL, 1, 0, 'F', '0', '0', 'project:productGene:remove', '#', 1, NOW(), '删除产品关联基因'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:productGene:remove') AS c3);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.parent_id = @menu_id AND m.perms LIKE 'project:productGene:%';
