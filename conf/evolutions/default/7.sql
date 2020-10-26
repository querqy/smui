# --- !Ups

-- lock_time - will be set to the date/time the migration starts, for potential handling (timeouts) of broken migrations
-- completed - shall be null or 1
create table smui_migration_lock (
	migration_key varchar(32) not null primary key,
	lock_time timestamp not null,
	completed int
);

# --- !Downs

drop table smui_migration_lock;
