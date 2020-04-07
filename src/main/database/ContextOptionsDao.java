package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class ContextOptionsDao {
	@Getter private static List<ContextOptionsDto> allContextOptions;
	
	private ContextOptionsDao() {};
	
	public static void cacheAllContextOptions() {
		allContextOptions = new ArrayList<>();
		final String query = "select id, name, priority from context_options";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					allContextOptions.add(new ContextOptionsDto(rs.getInt("id"), rs.getString("name"), rs.getInt("priority")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
