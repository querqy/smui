# --- First database schema

# --- !Ups

create table solr_index (
	id bigint not null auto_increment,
	name varchar(1000) not null,
	description varchar(1000) not null,
	last_update timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
	constraint pk_solr_index primary key (id)
);

create table search_input (
	id bigint not null auto_increment,
	term varchar(1000) not null,
	solr_index_id bigint not null,
	last_update timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
	constraint pk_search_input primary key (id),
	foreign key (solr_index_id) references solr_index(id) on delete cascade
);

create table synonym_rule (
	id bigint not null auto_increment,
	synonym_type int not null,
	term varchar(1000) not null,
	search_input_id bigint not null,
	last_update timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
	constraint pk_synonym_rule primary key (id),
	foreign key (search_input_id) references search_input(id) on delete cascade
);
-- synonym_type
-- ~~~~~~~~~~~~
-- 0 - undirected synonym
-- 1 - directed synonym

create table up_down_rule (
	id bigint not null auto_increment,
	up_down_type int not null,
	boost_malus_value int not null,
	term varchar(1000) not null,
	search_input_id bigint not null,
	last_update timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
	constraint pk_up_down_rule primary key (id),
	foreign key (search_input_id) references search_input(id) on delete cascade
);
-- up_down_type
-- ~~~~~~~~~~~~
-- 0 - UP
-- 1 - DOWN

create table filter_rule (
	id bigint not null auto_increment,
	term varchar(1000) not null,
	search_input_id bigint not null,
	last_update timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
	constraint pk_filter_rule primary key (id),
	foreign key (search_input_id) references search_input(id) on delete cascade
);

create table delete_rule (
	id bigint not null auto_increment,
	term varchar(1000) not null,
	search_input_id bigint not null,
	last_update timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
	constraint pk_delete_rule primary key (id),
	foreign key (search_input_id) references search_input(id) on delete cascade
);

create table suggested_solr_field (
	id bigint not null auto_increment,
	name varchar(1000) not null,
	solr_index_id bigint not null,
	last_update timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
	constraint pk_suggested_solr_fields primary key (id),
	foreign key (solr_index_id) references solr_index(id) on delete cascade
);

# --- !Downs

SET FOREIGN_KEY_CHECKS = 0;

drop table if exists suggested_solr_field;

drop table if exists delete_rule;

drop table if exists filter_rule;

drop table if exists up_down_rule;

drop table if exists synonym_rule;

drop table if exists search_input;

drop table if exists solr_index;

SET FOREIGN_KEY_CHECKS = 1;
