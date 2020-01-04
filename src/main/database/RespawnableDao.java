package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import lombok.Getter;

public class RespawnableDao {
	@Getter private static ArrayList<RespawnableDto> cachedRespawnables;
	
	public static RespawnableDto getCachedRespawnableByRoomIdTileId(int roomId, int tileId) {
		for (RespawnableDto dto : cachedRespawnables) {
			if (dto.getRoomId() == roomId && dto.getTileId() == tileId)
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
			cachedRespawnables = new ArrayList<>();
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					cachedRespawnables.add(new RespawnableDto(
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
