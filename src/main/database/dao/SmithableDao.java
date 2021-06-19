package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import main.database.DbConnection;
import main.database.dto.SmithableDto;

public class SmithableDao {
	private static Map<Integer, SmithableDto> smithables;
	
	public static void setupCaches() {
		smithables = new HashMap<>();
		
		final String query = "select item_id, level, bar_id, required_bars from smithable";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					smithables.put(rs.getInt("item_id"), new SmithableDto(
							rs.getInt("item_id"),
							rs.getInt("level"),
							rs.getInt("bar_id"),
							rs.getInt("required_bars")
					));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static List<SmithableDto> getAllItemsByBarId(int barId) {
		return smithables.values().stream().filter(e -> e.getBarId() == barId).collect(Collectors.toList());
	}
	
	public static SmithableDto getSmithableItemByItemId(int itemId) {
		return smithables.get(itemId);
	}
}
