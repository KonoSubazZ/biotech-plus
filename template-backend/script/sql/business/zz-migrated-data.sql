
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

LOCK TABLES `qc_standard` WRITE;
/*!40000 ALTER TABLE `qc_standard` DISABLE KEYS */;
INSERT IGNORE INTO `qc_standard` (`id`, `product_id`, `qc_item`, `qc_category`, `min_value`, `max_value`, `unit`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `tenant_id`) VALUES (5,2,'row_base','bioinfo','2',NULL,NULL,'active',NULL,1,'2026-10-03 00:03:54',1,'2026-10-03 00:03:54','0','000000');
INSERT IGNORE INTO `qc_standard` (`id`, `product_id`, `qc_item`, `qc_category`, `min_value`, `max_value`, `unit`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `tenant_id`) VALUES (6,2,'average_depth','bioinfo','95',NULL,NULL,'active',NULL,1,'2026-10-03 00:04:40',1,'2026-10-03 00:04:40','0','000000');
INSERT IGNORE INTO `qc_standard` (`id`, `product_id`, `qc_item`, `qc_category`, `min_value`, `max_value`, `unit`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `tenant_id`) VALUES (7,2,'coverage_target','bioinfo','99',NULL,NULL,'active',NULL,1,'2026-10-03 00:05:01',1,'2026-10-03 00:05:01','0','000000');
INSERT IGNORE INTO `qc_standard` (`id`, `product_id`, `qc_item`, `qc_category`, `min_value`, `max_value`, `unit`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `tenant_id`) VALUES (8,2,'probe_capture_ratio','bioinfo','70',NULL,NULL,'active',NULL,1,'2026-10-03 00:05:14',1,'2026-10-03 00:05:14','0','000000');
INSERT IGNORE INTO `qc_standard` (`id`, `product_id`, `qc_item`, `qc_category`, `min_value`, `max_value`, `unit`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `tenant_id`) VALUES (9,2,'peak_size','bioinfo','160',NULL,NULL,'active',NULL,1,'2026-10-03 00:05:30',1,'2026-10-03 00:05:30','0','000000');
/*!40000 ALTER TABLE `qc_standard` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `product_config` WRITE;
/*!40000 ALTER TABLE `product_config` DISABLE KEYS */;
INSERT IGNORE INTO `product_config` (`id`, `name`, `code`, `test_type`, `related_diseases`, `report_cycle_days`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `tenant_id`) VALUES (2,'novopm2_tis_1238_shengyu','BTP001','组织','肺癌、肠癌',20,'active','圣域测试产品',1,'2026-10-02 23:52:37',1,'2026-10-02 23:52:37','0','000000');
/*!40000 ALTER TABLE `product_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

