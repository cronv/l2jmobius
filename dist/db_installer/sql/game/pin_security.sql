CREATE TABLE IF NOT EXISTS `pin_security` (
	`pin_scope` varchar(16) NOT NULL DEFAULT '',
	`account_name` varchar(45) NOT NULL DEFAULT '',
	`character_id` int(10) NOT NULL DEFAULT '0',
	`pin_hash` varchar(128) NOT NULL DEFAULT '',
	`pin_salt` varchar(64) NOT NULL DEFAULT '',
	`failed_attempts` int(3) NOT NULL DEFAULT '0',
	`locked_until` bigint(20) NOT NULL DEFAULT '0',
	`created_at` bigint(20) NOT NULL DEFAULT '0',
	`updated_at` bigint(20) NOT NULL DEFAULT '0',
	`last_attempt` bigint(20) NOT NULL DEFAULT '0',
	PRIMARY KEY (`pin_scope`,`account_name`,`character_id`),
	KEY `account_name` (`account_name`),
	KEY `character_id` (`character_id`)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
