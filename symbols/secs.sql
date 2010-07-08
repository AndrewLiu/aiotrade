-- MySQL dump 10.13  Distrib 5.5.4-m3, for portbld-freebsd8.1 (amd64)
--
-- Host: localhost    Database: faster
-- ------------------------------------------------------
-- Server version	5.5.4-m3-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `secs`
--

DROP TABLE IF EXISTS `secs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `secs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `exchanges_id` bigint(20) NOT NULL,
  `validFrom` bigint(20) NOT NULL,
  `validTo` bigint(20) NOT NULL,
  `companies_id` bigint(20) NOT NULL,
  `secInfos_id` bigint(20) NOT NULL,
  `secStatuses_id` bigint(20) NOT NULL,
  `secIssues_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `secs_exchanges_id_fkey` (`exchanges_id`),
  KEY `secs_companies_id_fkey` (`companies_id`),
  KEY `secs_secInfos_id_fkey` (`secInfos_id`),
  KEY `secs_secStatuses_id_fkey` (`secStatuses_id`),
  KEY `secs_secIssues_id_fkey` (`secIssues_id`),
  CONSTRAINT `secs_companies_id_fkey` FOREIGN KEY (`companies_id`) REFERENCES `companies` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `secs_exchanges_id_fkey` FOREIGN KEY (`exchanges_id`) REFERENCES `exchanges` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `secs_secInfos_id_fkey` FOREIGN KEY (`secInfos_id`) REFERENCES `sec_infos` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `secs_secIssues_id_fkey` FOREIGN KEY (`secIssues_id`) REFERENCES `sec_issues` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `secs_secStatuses_id_fkey` FOREIGN KEY (`secStatuses_id`) REFERENCES `sec_statuses` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=5447 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2010-07-07 22:27:51
