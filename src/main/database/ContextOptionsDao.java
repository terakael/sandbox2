package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ContextOptionsDao {
	private ContextOptionsDao() {};
	
	public static Map<Integer, String> getAllContextOptions() {
		final String query = "select id, name from context_options";
		Map<Integer, String> contextOptions = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					contextOptions.put(rs.getInt("id"), rs.getString("name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return contextOptions;
	}
}
