# --- !Ups

-- Add canonical_spelling and alternative_spelling table.

create table canonical_spelling (
	id varchar(36) not null primary key,
	solr_index_id varchar(36),
	term varchar(1000) not null,
	status int not null,
	comment varchar(1000) not null,
	last_update timestamp not null
);

-- Add table alternative_spelling

create table alternative_spelling (
    id varchar(36) not null primary key,
	canonical_spelling_id varchar(36) not null,
	term varchar(1000) not null,
	status int not null,
	last_update timestamp not null
);

create index alternative_canonical_spelling_id_index on alternative_spelling (canonical_spelling_id);

# --- !Downs

drop table canonical_spelling;
drop table alternative_spelling;