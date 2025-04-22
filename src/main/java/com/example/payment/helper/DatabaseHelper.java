package com.example.payment.helper;

import com.example.payment.logging.MyLogger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class DatabaseHelper {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcTemplate jdbc;

    private final MyLogger logger = new MyLogger(LoggerFactory.getLogger(DatabaseHelper.class));

    public DatabaseHelper(NamedParameterJdbcTemplate jdbcTemplate, JdbcTemplate jdbc) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbc = jdbc;
    }

    public boolean doesTableExist(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = :tableName";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("tableName", tableName);

        Integer count = jdbcTemplate.queryForObject(sql, parameters, Integer.class);

        return count != null && count > 0;
    }

    public void printInfo() throws SQLException {
        DataSource dataSource = jdbc.getDataSource();
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();

                logger.info("Database: " + metaData.getDatabaseProductName());
                logger.info("URL: " + metaData.getURL());

                logger.info("Tables in DB:", jdbc.queryForList("SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_schema='PUBLIC'", String.class));
            }
        }
    }

    public Integer countPayment() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment", Collections.emptyMap(), Integer.class);
    }

    public void cleanDatabase() {
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");

        List<String> tables = getAllTables();

        for (String table : tables) {
            jdbc.execute("TRUNCATE TABLE " + table);
        }

        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private List<String> getAllTables() {
        return jdbc.queryForList("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'", String.class);
    }
}
