# --- !Ups


-- users are grouped together into teams, and teams have access to solr_index aka rules collection.
-- inspired by https://pedrorijo.com/blog/scala-play-auth/

create table user (
	id varchar(36) not null primary key,
	username varchar(50) not null,
	email varchar(50) not null,
	password varchar(50) not null,
    admin int not null,
    last_update timestamp not null
);

create table team (
	id varchar(36) not null primary key,
	name varchar(50) not null
);

create table user_2_team (
  user_id varchar(36) not null,
  team_id varchar(36) not null,
  last_update timestamp not null,
  primary key (user_id, team_id)
);

create table team_2_solr_index (
  team_id varchar(36) not null,
  solr_index_id varchar(36) not null,
  last_update timestamp not null,
  primary key (team_id, solr_index_id)
);

# --- !Downs

drop table user;
drop table team;
drop table user_2_team;
drop table team_2_solr_index;
