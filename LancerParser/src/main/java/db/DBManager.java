package db;

import java.sql.*;

public class DBManager {
    public Connection conn = null;
    public Statement stat = null;

    private String connURL = "jdbc:postgresql://localhost:5432/bigclonebench";
    private String userName = "postgres";
    private String password = "postgres";

    public DBManager() {
        initDBConnection();
    }

    public DBManager(String connURL, String userName, String password) {
        this.connURL = connURL;
        this.userName = userName;
        this.password = password;

        initDBConnection();
    }

    private void initDBConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(connURL, userName, password);
            conn.setAutoCommit(false);
            stat = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ResultSet executeQuery(String sql) {
        try {
            return this.stat.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
