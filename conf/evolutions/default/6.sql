# --- !Ups

-- event_type (for search_input and rule)
-- ~~~~~~~~~~
-- 0 - created
-- 1 - updated / changed (event entry also present, if only associated rules for input change)
-- 2 - deleted

-- status - no change is indicated by -1
-- tag_payload - contains a (serialized JSON) list of all assigned tags
-- input_type:
-- ~~~~~~~~~~~
-- 0 - search input
-- 1 - spelling
-- (see app/search-management.model.ts :: ListItemType)

create table eventhistory_search_input (
	id varchar(36) not null primary key,
	search_input_id varchar(36) not null,
	event_type int not null,
	event_time timestamp not null,
	user_info varchar(1000),
	term varchar(1000),
	status int,
	input_type int,
	comment varchar(1000),
	tag_payload varchar(2000)
);

-- term - can be null for REDIRECT rule
-- rule_type - SYNONYM, UP_DOWN, FILTER, REDIRECT (as described on the frontend)
-- prm_payload - contains all rule details as serialized JSON (synonym direction, boost/malus value, etc.) BEFORE modification

create table eventhistory_rule (
	id varchar(36) not null primary key,
	input_event_id varchar(36) not null,
	event_type int not null,
	event_time timestamp not null,
	term varchar(1000),
	status int,
	rule_type varchar(100) not null,
	rule_id varchar(36) not null,
	prm_payload varchar(2000)
);

# --- !Downs

drop table eventhistory_search_input;
drop table eventhistory_rule;
