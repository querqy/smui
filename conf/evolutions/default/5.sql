# --- !Ups

-- Add canonical_spelling and alternate_spelling table.

create table canonical_spelling (
	id varchar(36) not null primary key,
	solr_index_id varchar(36),
	term varchar(1000) not null,
	last_update timestamp not null
);

-- Add table alternate_spelling

create table alternate_spelling (
    id varchar(36) not null primary key,
	canonical_spelling_id varchar(36) not null,
	term varchar(1000) not null,
	last_update timestamp not null
);

create index alternate_canonical_spelling_id_index on alternate_spelling (canonical_spelling_id);

# --- !Downs

drop table canonical_spelling;
drop table alternate_spelling;