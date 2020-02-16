import requests
import json
import uuid

SMUI_API_URL = 'http://localhost:9000'

# decide for the Solr index to append input and rules
# NOTE: A GET onto /api/v1/solr-index will result in all available indices, e.g.:
#> [{"id":"a4aaf472-c0c0-49ac-8e34-c70fef9aa8a9","name":"mySolrCore","description":"My Solr Core"}]
solr_index_id = 'a4aaf472-c0c0-49ac-8e34-c70fef9aa8a9'

# write search input (query to match the rule)
search_input = {
    'term': 'search query'
}
search_input_rawresult = requests.put(
    '{}/api/v1/{}/search-input'.format(SMUI_API_URL, solr_index_id),
    data=json.dumps(search_input),
    headers={'Content-Type': 'application/json'}
)
if not ('OK' in search_input_rawresult.text):
    print('Adding Search Input NOT successful. Stopping here!')
    exit()
search_input_id = json.loads(search_input_rawresult.text)['returnId']
# re-read search input to have all information / data structure complete
search_input_rawresult = requests.get(
    '{}/api/v1/search-input/{}'.format(SMUI_API_URL, search_input_id),
    headers={'Content-Type': 'application/json'}
)
search_input = json.loads(search_input_rawresult.text)

# add SYNONYM to input
search_input['synonymRules'].append({
    'id': '{}'.format(uuid.uuid4()),
    'synonymType': 0, # NOTE: This creates a undirected synonym
    'term': 'synonym for query',
    'isActive': True
})
# add FILTER to input
search_input['filterRules'].append({
    'id': '{}'.format(uuid.uuid4()),
    'term': '* searchField:filter value', # NOTE: * notation creates a filter using native Solr language
    'isActive': True
})
search_input_rawresult = requests.post(
    '{}/api/v1/search-input/{}'.format(SMUI_API_URL, search_input_id),
    data=json.dumps(search_input),
    headers={'Content-Type': 'application/json'}
)
if not ('OK' in search_input_rawresult.text):
    print('Updating Search Input NOT successful. Stopping here!')
    exit()