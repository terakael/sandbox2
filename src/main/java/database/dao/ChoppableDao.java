package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import database.dto.ChoppableDto;

public class ChoppableDao {
	private static Map<Integer, ChoppableDto> choppables = new HashMap<>(); // sceneryId, dto
	
	public static void setupCaches() {
		cacheChoppables();
	}
	
	private static void cacheChoppables() {
		final String query = "select scenery_id, level, exp, log_id, respawn_ticks from choppable";
		DbConnection.load(query, rs -> {
			choppables.put(rs.getInt("scenery_id"), new ChoppableDto(
					rs.getInt("scenery_id"),
					rs.getInt("level"),
					rs.getInt("exp"),
					rs.getInt("log_id"),
					rs.getInt("respawn_ticks")));
		});
	}
	
	public static ChoppableDto getChoppableBySceneryId(int sceneryId) {
		return choppables.get(sceneryId);
	}
	
	public static Set<ChoppableDto> getAllChoppables() {
		return new HashSet<>(choppables.values());
	}
}
