# --- !Ups

-- event_type (for input_event and rule_event)
-- ~~~~~~~~~~
-- 0 - created
-- 1 - updated / changed (event entry also present, if only associated rules for input change)
-- 2 - deleted
-- 3 - virtually_created (virtual created events for entities existing BEFORE SMUI v3.8)

-- event_source (source input of event)
-- ~~~~~~~~~~~
-- SearchInput
-- CanonicalSpelling
--
-- json_payload
-- ~~~~~~~~~~~~
-- contains complete input and associated rules (potential changes) as serialized JSON or null (in case of deleted).
-- payload is currently limited to approx 5K!

-- 2020-09-13: first encountered JSON payload with 5391B in production ==> double the field size
create table input_event (
	id varchar(36) not null primary key,
	event_source varchar(50) not null,
	event_type int not null,
	event_time timestamp not null,
	user_info varchar(100),
	input_id varchar(36) not null,
	json_payload varchar(10000)
);

# --- !Downs

drop table input_event;
