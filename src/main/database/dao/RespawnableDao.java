package main.database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import main.database.DbConnection;
import main.database.dto.RespawnableDto;

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
		DbConnection.load(query, rs -> {
			cachedRespawnables.putIfAbsent(rs.getInt("floor"), new ArrayList<>());
			cachedRespawnables.get(rs.getInt("floor")).add(new RespawnableDto(
					rs.getInt("floor"), 
					rs.getInt("tile_id"), 
					rs.getInt("item_id"), 
					rs.getInt("count"), 
					rs.getInt("respawn_ticks")));
		});
	}
}
