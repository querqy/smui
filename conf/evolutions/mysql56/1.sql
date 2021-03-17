# --- First database schema

# --- !Ups

create table solr_index (
	id varchar(36) not null primary key,
	name varchar(1000) not null,
	description varchar(1000) not null,
	last_update timestamp not null
);

create table search_input (
	id varchar(36) not null primary key,
	term varchar(1000) not null,
	solr_index_id varchar(36) not null,
	last_update timestamp not null
);

-- status (for synonym_rule, up_down_rule, filter_rule, delete_rule)
-- ~~~~~~
-- 0 - inactive (do not deploy)
-- 1 - active (deploy the rule normally)
-- ... other bits, reserved for future stati

-- synonym_type
-- ~~~~~~~~~~~~
-- 0 - undirected synonym
-- 1 - directed synonym
create table synonym_rule (
	id varchar(36) not null primary key,
	synonym_type int not null,
	term varchar(1000) not null,
	search_input_id varchar(36) not null,
	last_update timestamp not null,
	status int not null
);

-- up_down_type
-- ~~~~~~~~~~~~
-- 0 - UP
-- 1 - DOWN
create table up_down_rule (
	id varchar(36) not null primary key,
	up_down_type int not null,
	boost_malus_value int not null,
	term varchar(1000) not null,
	search_input_id varchar(36) not null,
	last_update timestamp not null,
	status int not null
);

create table filter_rule (
	id varchar(36) not null primary key,
	term varchar(1000) not null,
	search_input_id varchar(36) not null,
	last_update timestamp not null,
	status int not null
);

create table delete_rule (
	id varchar(36) not null primary key,
	term varchar(1000) not null,
	search_input_id varchar(36) not null,
	last_update timestamp not null,
	status int not null
);

create table suggested_solr_field (
	id varchar(36) not null primary key,
	name varchar(1000) not null,
	solr_index_id varchar(36) not null,
	last_update timestamp not null
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
create table deployment_log (
	id varchar(36) not null primary key,
	solr_index_id varchar(36) not null,
	target_platform varchar(10) not null,
    last_update timestamp not null,
    result int not null
);

# --- !Downs

drop table deployment_log;

drop table suggested_solr_field;

drop table delete_rule;

drop table filter_rule;

drop table up_down_rule;

drop table synonym_rule;

drop table search_input;

drop table solr_index;

