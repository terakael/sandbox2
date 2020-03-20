package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class TeleportableDao {
private static HashMap<Integer, TeleportableDto> teleportables; // itemId, dto
	
	public static void setupCaches() {
		final String query = "select item_id, room_id, tile_id from teleportable";
		
		teleportables = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					teleportables.put(rs.getInt("item_id"), new TeleportableDto(rs.getInt("item_id"), rs.getInt("room_id"), rs.getInt("tile_id")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isTeleportable(int itemId) {
		return teleportables.containsKey(itemId);
	}
	
	public static TeleportableDto getTeleportableByItemId(int itemId) {
		if (isTeleportable(itemId))
			return teleportables.get(itemId);
		return null;
	}
}
