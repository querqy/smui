-- empty existing data

--delete from delete_rule;
--delete from filter_rule;
--delete from up_down_rule;
--delete from synonym_rule;
--delete from search_input;
--delete from solr_index;

-- insert test data

insert into solr_index (name, description) values ('de_DE', 'de_DE - Deutscher Index');
insert into solr_index (name, description) values ('en_EN', 'en_EN - Englischer Index');

insert into search_input (term, solr_index_id) values ('arbeitsministerium', 1);
insert into search_input (term, solr_index_id) values ('Betriebsverfassungsgesetz', 1);
insert into search_input (term, solr_index_id) values ('FlexÜ', 1);
insert into search_input (term, solr_index_id) values ('manteltarifvertrag', 1);

insert into synonym_rule (synonym_type, term, search_input_id) values (0, 'Bundesministerium für Arbeit und Soziales', 1);
insert into synonym_rule (synonym_type, term, search_input_id) values (0, 'BMAS', 1);
insert into synonym_rule (synonym_type, term, search_input_id) values (0, 'BetrVG', 2);
insert into synonym_rule (synonym_type, term, search_input_id) values (0, 'Tarifvertrag flexiblen Übergang Rente', 3);
insert into synonym_rule (synonym_type, term, search_input_id) values (1, 'MTV', 4);
insert into synonym_rule (synonym_type, term, search_input_id) values (0, 'Manteltarifbestimmungen', 4);

insert into up_down_rule (up_down_type, boost_malus_value, term, search_input_id) values (0, 10, 'Altersteilzeit', 3);
insert into up_down_rule (up_down_type, boost_malus_value, term, search_input_id) values (0, 20, '* microline1:Vertrag', 3);
insert into up_down_rule (up_down_type, boost_malus_value, term, search_input_id) values (0, 100, '* microline1:Vertrag', 4);
insert into up_down_rule (up_down_type, boost_malus_value, term, search_input_id) values (0, 2, 'Tarifkompendium', 4);
insert into up_down_rule (up_down_type, boost_malus_value, term, search_input_id) values (1, 500, 'englische Übersetzung', 4);

insert into suggested_solr_field (name, solr_index_id) values ('microline1', 1);
insert into suggested_solr_field (name, solr_index_id) values ('microline1', 2);
