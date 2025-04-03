package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbcp.BasicDataSource;

// connection pool: https://stackoverflow.com/questions/7592056/am-i-using-jdbc-connection-pooling

public class DbConnection {
	private static final BasicDataSource dataSource = new BasicDataSource();
	static {
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://pc.bearded-quail.ts.net:3306/local_schema?autoReconnect=true");
		dataSource.setUsername("danscape");
		dataSource.setPassword("danscape");
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

	public static void load(String query, ThrowingConsumer<ResultSet, SQLException> fn) {
		try (
				Connection connection = DbConnection.get();
				PreparedStatement ps = connection.prepareStatement(query);) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					fn.accept(rs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void load(String query, ThrowingConsumer<ResultSet, SQLException> fn, Integer... params) {
		try (
				Connection connection = DbConnection.get();
				PreparedStatement ps = connection.prepareStatement(query);) {
			for (int i = 0; i < params.length; ++i)
				ps.setInt(i + 1, params[i]);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					fn.accept(rs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
