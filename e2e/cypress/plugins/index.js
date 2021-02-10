const pg = require('pg');
const cypressConfig = require("../../cypress.json");

module.exports = (on, _) => {
  on('task', {
    query({ sql, values }) {
      const pool = new pg.Pool(cypressConfig.db);
      try {
        return pool.query(sql, values);
      } catch (e) {
        console.error(e);
      }
    }
  });
};
