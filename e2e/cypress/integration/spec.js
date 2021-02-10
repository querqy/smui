import { truncateTable } from '../src/sql';

context('SMUI app', () => {
  beforeEach(() => {
    truncateTable('search_input');
    cy.visit('/');
  });

  it('should have a page headline', () => {
    cy.get('.navbar-brand').should('have.text', 'SMUI demo installation');
  });

  it('should create a new rule item and sort it in rules list', () => {
    cy.get('[data-test=rules-search-input]').type('testRuleB{enter}');
    cy.get('.list-item-term').should(terms => {
      expect(terms).to.have.length(1);
      expect(terms.eq(0).text()).to.contain('testRuleB');
    });

    cy.get('[data-test=rules-search-input]').type('testRuleC{enter}');
    cy.get('.list-item-term').should(terms => {
      expect(terms).to.have.length(2);
      expect(terms.eq(0).text()).to.contain('testRuleB');
      expect(terms.eq(1).text()).to.contain('testRuleC');
    });

    cy.get('[data-test=rules-search-input]').type('testRuleA{enter}');
    cy.get('.list-item-term').should(terms => {
      expect(terms).to.have.length(3);
      expect(terms.eq(0).text()).to.contain('testRuleA');
      expect(terms.eq(1).text()).to.contain('testRuleB');
      expect(terms.eq(2).text()).to.contain('testRuleC');
    });
  });
});
