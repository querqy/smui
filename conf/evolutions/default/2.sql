# --- v1.4 database schema updates - adding features like: deployment logs, inactive rules

# --- !Ups

create table deployment_log (
	id bigint not null auto_increment,
	target_platform varchar(10),
    last_update timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    result int default 0,
	constraint pk_search_input primary key (id)
);
-- target_platform
-- ~~~~~~~~~~~~~~~
-- 'PRELIVE' - deployment done to a staging or pre-live environment (only possible, if activated, see README.md)
-- 'LIVE' - deployment done to the live environment
--
-- result
-- ~~~~~~
-- 0 - OK
-- 1 - ERROR

alter table synonym_rule
add status int default 1;

alter table up_down_rule
add status int default 1;

alter table filter_rule
add status int default 1;

alter table delete_rule
add status int default 1;

-- status
-- ~~~~~~
-- 0 - inactive (do not deploy)
-- 1 - active (deploy the rule normally)
-- ... other bits, reserved for future stati

# --- !Downs

SET FOREIGN_KEY_CHECKS = 0;

drop table if exists deployment_log;

SET FOREIGN_KEY_CHECKS = 1;
