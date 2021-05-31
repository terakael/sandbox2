package main.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;

// connection pool: https://stackoverflow.com/questions/7592056/am-i-using-jdbc-connection-pooling

public class DbConnection {
	private static final BasicDataSource dataSource = new BasicDataSource();
	static {
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://localhost:3306/local_schema?autoReconnect=true&useSSL=false");
		dataSource.setUsername("root");
		dataSource.setPassword("ppL34kn4g");
		System.out.println("datasource set");
	}
	
	private DbConnection() {
		//
	}
	
	public static Connection get() {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			System.out.println(e);
			return null;
		}
	}
}
