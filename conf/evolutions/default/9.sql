# --- !Ups

create table something (
                              id varchar(36) not null primary key,
                              value0 varchar(1000) not null,
                              last_update timestamp not null
);

# --- !Downs

drop table something;