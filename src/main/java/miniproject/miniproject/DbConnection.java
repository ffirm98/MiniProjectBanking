package miniproject.miniproject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DbConnection {

    private static final Logger logger = LoggerFactory.getLogger(DbConnection.class);

    @Autowired
    private DataSource dataSource;

    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    protected void close(Connection conn, Statement st, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { logger.error(e.getMessage(), e); }
        try { if (st != null) st.close(); } catch (SQLException e) { logger.error(e.getMessage(), e); }
        try { if (conn != null) conn.close(); } catch (SQLException e) { logger.error(e.getMessage(), e); }
    }
}