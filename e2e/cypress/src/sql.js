const truncateTable = tableName => {
  cy.task('query', {
    sql: `truncate table ${tableName}`
  });
};

export { truncateTable };
