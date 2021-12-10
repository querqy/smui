//import { truncateTable } from '../src/sql';

context('SMUI app', () => {
  beforeEach(() => {
    //truncateTable('search_input');
    cy.visit('/');
  });

  it('should be able to create a new Rules Collection', () => {
    cy.contains('Admin').click();

    var listingCount = 0;
    cy.get('app-smui-admin-rules-collection-list .list-group-item').should(collections => {
      listingCount = collections.length;
    });

    cy.get('#collectionName').type('testRulesCollection');
    cy.get('#collectionSearchEngineName').type('test_search_engine');
    cy.get('#createRulesCollectionBtn').click();

    cy.wait(2000)

    cy.get('app-smui-admin-rules-collection-list .list-group-item').should(collections => {
      expect(collections).to.have.length(listingCount + 1);
    });
  });
});
