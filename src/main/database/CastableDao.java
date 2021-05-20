package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import lombok.Getter;

public class CastableDao {
	private static HashMap<Integer, CastableDto> castables; // itemId, dto
	
	public static void setupCaches() {
		cacheCastables();
	}
	
	private static void cacheCastables() {
		final String query = "select item_id, level, exp, max_hit, sprite_frame_id from castable";
		
		castables = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					castables.put(rs.getInt("item_id"), new CastableDto(rs.getInt("item_id"), rs.getInt("level"), rs.getInt("exp"), rs.getInt("max_hit"), rs.getInt("sprite_frame_id")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	
	public static boolean isCastable(int itemId) {
		return castables.containsKey(itemId);
	}
	
	public static CastableDto getCastableByItemId(int itemId) {
		return castables.get(itemId);
	}
}
