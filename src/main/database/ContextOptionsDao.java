package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContextOptionsDao {
	private ContextOptionsDao() {};
	
	public static List<ContextOptionsDto> getAllContextOptions() {
		final String query = "select id, name, priority from context_options";
		List<ContextOptionsDto> contextOptions = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					contextOptions.add(new ContextOptionsDto(rs.getInt("id"), rs.getString("name"), rs.getInt("priority")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return contextOptions;
	}
}
