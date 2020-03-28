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
	
	public static RespawnableDto getCachedRespawnableByFloorAndTileId(int floor, int tileId) {
		if (!cachedRespawnables.containsKey(floor))
			return null;
		
		for (RespawnableDto dto : cachedRespawnables.get(floor)) {
			if (dto.getTileId() == tileId)
				return dto;
		}
		return null;
	}
	
	public static void setupCaches() {
		final String query = "select floor, tile_id, item_id, count, respawn_ticks from respawnable ";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (!cachedRespawnables.containsKey(rs.getInt("floor")))
						cachedRespawnables.put(rs.getInt("floor"), new ArrayList<>());
					
					cachedRespawnables.get(rs.getInt("floor")).add(new RespawnableDto(
							rs.getInt("floor"), 
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
