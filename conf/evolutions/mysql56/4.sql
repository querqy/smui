# --- !Ups

-- Add status (for deactivating) to search_input

-- status (for search_input)
-- ~~~~~~
-- 0 - inactive (do not deploy input and all its rules)
-- 1 - active (deploy the inputs and rules normally)
-- ... other bits, reserved for future stati

alter table search_input add status int not null default 1;
alter table search_input add comment varchar(1000) not null default '';

# --- !Downs
