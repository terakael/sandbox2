package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import main.database.DbConnection;
import main.database.dto.SawmillableDto;

public class SawmillableDao {
	private static Map<Integer, SawmillableDto> sawmillableByLogId;
	private static Map<Integer, SawmillableDto> sawmillableByResultingPlankId;
	
	public static void setupCaches() {
		cacheSawmillable();
	}
	
	private static void cacheSawmillable() {
		final String query = "select log_id, required_level, exp, resulting_plank_id from sawmillable";
		
		sawmillableByLogId = new HashMap<>();
		sawmillableByResultingPlankId = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final SawmillableDto dto = new SawmillableDto(rs.getInt("log_id"), rs.getInt("required_level"), rs.getInt("exp"), rs.getInt("resulting_plank_id"));
					sawmillableByLogId.put(rs.getInt("log_id"), dto);
					sawmillableByResultingPlankId.put(rs.getInt("resulting_plank_id"), dto);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
