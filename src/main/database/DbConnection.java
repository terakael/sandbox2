package main.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {
	private static DbConnection connection;
	private Connection conn;
	private DbConnection(Connection conn) {
		this.conn = conn;
	}
	
	public static Connection get() {
		if (connection == null) {
			try {
				connection = new DbConnection(DriverManager.getConnection(
								"jdbc:mysql://localhost:3306/local_schema?autoReconnect=true&useSSL=false", 
								"root", 
								"ey3a4y3e"));
			} catch (SQLException e) {
				 assert(false);
			}
		}
		
		return connection.conn;
	}
}
