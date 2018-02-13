/*
Navicat MySQL Data Transfer

Source Server         : test
Source Server Version : 50173
Source Host           : 127.0.0.1:20001
Source Database       : kurento

Target Server Type    : MYSQL
Target Server Version : 50173
File Encoding         : 65001

Date: 2018-02-13 18:13:53
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `kurento`
-- ----------------------------
DROP TABLE IF EXISTS `kurento`;
CREATE TABLE `kurento` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `pipeline_id` varchar(64) NOT NULL,
  `webrtc_endpoint_id` varchar(128) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=28 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of kurento
-- ----------------------------
INSERT INTO `kurento` VALUES ('1', 'f620bbd8-de8a-4e19-9f09-95a030ff6e22_kurento.MediaPipeline', 'f620bbd8-de8a-4e19-9f09-95a030ff6e22_kurento.MediaPipeline/f1969913-558f-45ee-b10b-25f7601d8e3c_kurento.WebRtcEndpoint');

-- ----------------------------
-- Table structure for `viewers`
-- ----------------------------
DROP TABLE IF EXISTS `viewers`;
CREATE TABLE `viewers` (
  `id` bigint(20) NOT NULL,
  `session_id` varchar(64) NOT NULL,
  `webrtc_endpoint_id` varchar(128) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of viewers
-- ----------------------------
