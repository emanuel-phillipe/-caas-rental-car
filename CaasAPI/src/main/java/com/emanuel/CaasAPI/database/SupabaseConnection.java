package com.emanuel.CaasAPI.database;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class SupabaseConnection {
    private final DataSource dataSource;

    public SupabaseConnection(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection open() throws SQLException {
        return dataSource.getConnection();
    }
}
