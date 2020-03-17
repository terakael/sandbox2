package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public class RespawnableDao {
	@Getter private static Map<Integer, ArrayList<RespawnableDto>> cachedRespawnables = new HashMap<>();
	
	public static RespawnableDto getCachedRespawnableByRoomIdTileId(int roomId, int tileId) {
		if (!cachedRespawnables.containsKey(roomId))
			return null;
		
		for (RespawnableDto dto : cachedRespawnables.get(roomId)) {
			if (dto.getTileId() == tileId)
				return dto;
		}
		return null;
	}
	
	public static void setupCaches() {
		final String query = "select room_id, tile_id, item_id, count, respawn_ticks from respawnable ";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (!cachedRespawnables.containsKey(rs.getInt("room_id")))
						cachedRespawnables.put(rs.getInt("room_id"), new ArrayList<>());
					
					cachedRespawnables.get(rs.getInt("room_id")).add(new RespawnableDto(
							rs.getInt("room_id"), 
							rs.getInt("tile_id"), 
							rs.getInt("item_id"), 
							rs.getInt("count"), 
							rs.getInt("respawn_ticks")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
