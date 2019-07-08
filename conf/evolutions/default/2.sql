# --- !Ups

-- Adding REDIRECT rule table

create table redirect_rule (
	id varchar(36) not null primary key,
	target varchar(4096) not null,
	search_input_id varchar(36) not null,
	last_update timestamp not null,
	status int not null
);

# --- !Downs

drop table redirect_rule;

