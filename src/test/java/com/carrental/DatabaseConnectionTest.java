package com.carrental;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class để kiểm tra kết nối database MySQL
 */
@SpringBootTest
public class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Test kết nối cơ bản với database
     */
    @Test
    public void testDatabaseConnection() {
        assertNotNull(dataSource, "DataSource không được null");

        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Connection không được null");
            assertTrue(connection.isValid(2), "Connection không hợp lệ");
            assertFalse(connection.isClosed(), "Connection không nên đóng");

            System.out.println("✓ Kết nối database thành công!");
            System.out.println("Database URL: " + connection.getMetaData().getURL());
            System.out.println("Database User: " + connection.getMetaData().getUserName());
            System.out.println("Database Product: " + connection.getMetaData().getDatabaseProductName());
            System.out.println("Database Version: " + connection.getMetaData().getDatabaseProductVersion());

        } catch (SQLException e) {
            fail("Không thể kết nối đến database: " + e.getMessage());
        }
    }

    /**
     * Test thực thi query đơn giản
     */
    @Test
    public void testSimpleQuery() {
        assertNotNull(jdbcTemplate, "JdbcTemplate không được null");

        try {
            // Test query đơn giản để kiểm tra connection
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            assertEquals(1, result, "Query SELECT 1 phải trả về 1");

            System.out.println("✓ Thực thi query thành công!");

        } catch (Exception e) {
            fail("Không thể thực thi query: " + e.getMessage());
        }
    }

    /**
     * Test kiểm tra database có tồn tại không
     */
    @Test
    public void testDatabaseExists() {
        try {
            String databaseName = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
            );

            assertNotNull(databaseName, "Database name không được null");
            assertEquals("car_rental_system", databaseName, "Database name phải là car_rental_system");

            System.out.println("✓ Database '" + databaseName + "' tồn tại!");

        } catch (Exception e) {
            fail("Không thể kiểm tra database: " + e.getMessage());
        }
    }

    /**
     * Test liệt kê các tables trong database
     */
    @Test
    public void testListTables() {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'car_rental_system'";

            Integer tableCount = jdbcTemplate.queryForObject(sql, Integer.class);

            assertNotNull(tableCount, "Table count không được null");
            assertTrue(tableCount >= 0, "Table count phải >= 0");

            System.out.println("✓ Tìm thấy " + tableCount + " bảng trong database!");

            // Liệt kê tất cả các bảng
            String listSql = "SELECT table_name FROM information_schema.tables " +
                           "WHERE table_schema = 'car_rental_system'";

            jdbcTemplate.query(listSql, (rs, rowNum) -> {
                String tableName = rs.getString("table_name");
                System.out.println("  - " + tableName);
                return tableName;
            });

        } catch (Exception e) {
            fail("Không thể liệt kê tables: " + e.getMessage());
        }
    }

    /**
     * Test connection pool
     */
    @Test
    public void testConnectionPool() {
        try {
            // Tạo nhiều connections để test pool
            for (int i = 0; i < 5; i++) {
                try (Connection connection = dataSource.getConnection()) {
                    assertTrue(connection.isValid(2), "Connection " + (i+1) + " không hợp lệ");
                    System.out.println("✓ Connection " + (i+1) + " hoạt động tốt");
                }
            }

            System.out.println("✓ Connection pool hoạt động tốt!");

        } catch (SQLException e) {
            fail("Connection pool không hoạt động: " + e.getMessage());
        }
    }
}
