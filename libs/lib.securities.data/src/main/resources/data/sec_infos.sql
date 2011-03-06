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
-- Table structure for table `sec_infos`
--

DROP TABLE IF EXISTS `sec_infos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sec_infos` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `secs_id` bigint(20) NOT NULL,
  `validFrom` bigint(20) NOT NULL,
  `validTo` bigint(20) NOT NULL,
  `uniSymbol` varchar(10) NOT NULL DEFAULT '',
  `name` varchar(40) NOT NULL DEFAULT '',
  `totalShare` bigint(20) NOT NULL,
  `freeFloat` bigint(20) NOT NULL,
  `tradingUnit` int(11) NOT NULL,
  `upperLimit` float(12,2) NOT NULL,
  `lowerLimit` float(12,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `sec_infos_secs_id_fkey` (`secs_id`),
  CONSTRAINT `sec_infos_secs_id_fkey` FOREIGN KEY (`secs_id`) REFERENCES `secs` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=13942 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2010-09-09 12:09:07
