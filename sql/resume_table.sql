-- ----------------------------
-- 简历模块数据库初始化脚本
-- 运行前请确保 MySQL 服务已启动
-- 用法: mysql -u root -p < resume_table.sql
-- ----------------------------

-- ----------------------------
-- 1、创建数据库（如已存在则跳过）
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `ry_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ry_ai`;

-- ----------------------------
-- 2、简历表（resume）
-- ----------------------------
DROP TABLE IF EXISTS `resume`;
CREATE TABLE `resume` (
  `id`          bigint NOT NULL AUTO_INCREMENT  COMMENT '简历ID',
  `user_id`     bigint NOT NULL                  COMMENT '用户ID',
  `template_id` varchar(20) DEFAULT 'modern'     COMMENT '模板：modern/classic/minimal',
  `title`       varchar(100) DEFAULT NULL        COMMENT '简历标题',
  `content`     json NOT NULL                    COMMENT '完整简历内容(JSON)',
  `is_default`  tinyint DEFAULT 0                COMMENT '是否默认简历(0-否 1-是)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='简历表';

-- ----------------------------
-- 3、导航菜单（sys_menu）
-- 注意：若菜单ID已存在，请自行调整 menu_id 避免冲突
-- ----------------------------

-- 3.1 简历管理主目录（父菜单）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2000, '简历管理', 0, 5, 'resume', NULL, NULL, 'Resume', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', sysdate(), '', NULL, '简历管理目录');

-- 3.2 我的简历页面（子菜单）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2001, '我的简历', 2000, 1, 'resume/list', 'resume/list/index', NULL, 'ResumeList', 1, 0, 'C', '0', '0', 'resume:list:list', 'list', 'admin', sysdate(), '', NULL, '我的简历菜单');

-- 3.3 简历编辑页面（子菜单）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2002, '简历编辑', 2000, 2, 'resume/edit', 'resume/edit/index', NULL, 'ResumeEdit', 1, 0, 'C', '1', '0', 'resume:list:edit', 'edit', 'admin', sysdate(), '', NULL, '简历编辑菜单（隐藏）');

-- 3.4 按钮权限
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2003, '简历新增', 2000, 3, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'resume:list:add', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2004, '简历修改', 2000, 4, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'resume:list:edit', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2005, '简历删除', 2000, 5, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'resume:list:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2006, '简历查询', 2000, 6, '', NULL, NULL, '', 1, 0, 'F', '0', '0', 'resume:list:query', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 4、将菜单授权给管理员角色（role_id=1）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2000);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2001);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2002);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2003);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2004);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2005);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 2006);
