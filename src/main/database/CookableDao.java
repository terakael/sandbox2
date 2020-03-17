package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class CookableDao {
	private static HashMap<Integer, CookableDto> cookables;// raw_item_id, dto
	
	public static void cacheCookables() {
		final String query = "select raw_item_id, cooked_item_id, level, exp, burnt_item_id from cookable";
		
		cookables = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					cookables.put(rs.getInt("raw_item_id"), new CookableDto(rs.getInt("raw_item_id"), rs.getInt("cooked_item_id"), rs.getInt("level"), rs.getInt("exp"), rs.getInt("burnt_item_id")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static CookableDto getCookable(int itemId) {
		if (cookables.containsKey(itemId))
			return cookables.get(itemId);
		return null;
	}
}
