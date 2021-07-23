package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import database.dto.SawmillableDto;

public class SawmillableDao {
	private static Map<Integer, SawmillableDto> sawmillableByLogId = new HashMap<>();
	private static Map<Integer, SawmillableDto> sawmillableByResultingPlankId = new HashMap<>();
	
	public static void setupCaches() {
		cacheSawmillable();
	}
	
	private static void cacheSawmillable() {
		final String query = "select log_id, required_level, exp, resulting_plank_id from sawmillable";
		DbConnection.load(query, rs -> {
			final SawmillableDto dto = new SawmillableDto(rs.getInt("log_id"), rs.getInt("required_level"), rs.getInt("exp"), rs.getInt("resulting_plank_id"));
			sawmillableByLogId.put(rs.getInt("log_id"), dto);
			sawmillableByResultingPlankId.put(rs.getInt("resulting_plank_id"), dto);
		});
	}
	
	public static SawmillableDto getSawmillableByLogId(int logId) {
		return sawmillableByLogId.get(logId);
	}
	
	public static SawmillableDto getSawmillableByResultingPlankId(int plankId) {
		return sawmillableByResultingPlankId.get(plankId);
	}
	
	public static Set<SawmillableDto> getSawmillable() {
		return new HashSet<>(sawmillableByLogId.values());
	}
}
