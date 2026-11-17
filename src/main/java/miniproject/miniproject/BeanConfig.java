package miniproject.miniproject;


import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

    @Configuration
    public class BeanConfig {

        @Value("${app.datasource.url}")
        private String url;

        @Value("${app.datasource.username}")
        private String username;

        @Value("${app.datasource.password:}")
        private String password;

        @Value("${app.datasource.driver-class-name}")
        private String driverClassName;

        @Value("${app.datasource.pool-name:MiniLedgerPool}")
        private String poolName;

        @Value("${app.datasource.maximum-pool-size:10}")
        private int maxPoolSize;

        @Value("${app.datasource.minimum-idle:2}")
        private int minIdle;

        @Value("${app.datasource.connection-timeout:30000}")
        private long connectionTimeout;

        @Bean
        public DataSource dataSource() {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(driverClassName);
            config.setPoolName(poolName);
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
            config.setConnectionTimeout(connectionTimeout);
            return new HikariDataSource(config);
        }

        @Bean
        public BCryptPasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
