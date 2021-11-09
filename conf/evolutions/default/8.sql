# --- !Ups

-- Ensure that we do not allow duplicate suggested solr fields for the same solr index.
create unique index suggested_solr_field_name_solr_index on suggested_solr_field (solr_index_id, name);

# --- !Downs

drop index suggested_solr_field_name_solr_index;
