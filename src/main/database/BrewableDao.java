package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class BrewableDao {
	private static HashMap<Integer, BrewableDto> brewables;
	
	public static void cacheBrewables() {
		final String query = "select potion_id, level, exp from brewable";
		
		brewables = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					brewables.put(rs.getInt("potion_id"), new BrewableDto(rs.getInt("potion_id"), rs.getInt("level"), rs.getInt("exp")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isBrewable(int id) {
		return brewables.containsKey(id);
	}
	
	public static BrewableDto getBrewableById(int id) {
		if (brewables.containsKey(id))
			return brewables.get(id);
		return null;
	}
	
	public static int getRequiredLevelById(int id) {
		if (brewables.containsKey(id))
			return brewables.get(id).getLevel();
		return 0;
	}
	
	public static int getExpById(int id) {
		if (brewables.containsKey(id))
			return brewables.get(id).getExp();
		return 0;
	}
}
