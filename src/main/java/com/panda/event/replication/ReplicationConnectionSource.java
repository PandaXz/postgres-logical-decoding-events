package com.panda.event.replication;

import org.postgresql.PGConnection;
import org.postgresql.PGProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ReplicationConnectionSource implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ReplicationEventProducer.class);

    private String jdbcUrl;
    private String username;
    private String password;
    private Connection connection;
    private boolean reconnectRequired = true;

    public ReplicationConnectionSource(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    //todo rewrite, use proxy to catch exceptions and invalidate connection
    public PGConnection getConnection(boolean reconnect) throws SQLException {
        if (reconnect) {
            closeConnection();
            reconnectRequired = true;
        }
        if (reconnectRequired || (connection != null && connection.isClosed()) || connection == null) {
            connection = createReplicationConnection();
        }
        return connection.unwrap(PGConnection.class);
    }

    public PGConnection getConnection() throws SQLException {
        return getConnection();
    }

    public void invalidateConnection(){
        reconnectRequired = true;
    }

    private Connection createReplicationConnection() throws SQLException {
        Properties props = new Properties();
        PGProperty.USER.set(props, username);
        PGProperty.PASSWORD.set(props, password);
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(props, "9.4");
        PGProperty.REPLICATION.set(props, "database");
        PGProperty.PREFER_QUERY_MODE.set(props, "simple");
        return DriverManager.getConnection(jdbcUrl, props);
    }

    private void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            closeConnection();
        } catch (SQLException e) {
        }
    }
}
