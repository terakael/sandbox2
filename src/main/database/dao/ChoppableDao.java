package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import main.database.DbConnection;
import main.database.dto.ChoppableDto;

public class ChoppableDao {
	private static Map<Integer, ChoppableDto> choppables; // sceneryId, dto
	
	public static void setupCaches() {
		cacheChoppables();
	}
	
	private static void cacheChoppables() {
		choppables = new HashMap<>();
		
		final String query = "select scenery_id, level, exp, log_id, respawn_ticks from choppable";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					choppables.put(rs.getInt("scenery_id"), new ChoppableDto(
							rs.getInt("scenery_id"),
							rs.getInt("level"),
							rs.getInt("exp"),
							rs.getInt("log_id"),
							rs.getInt("respawn_ticks")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static ChoppableDto getChoppableBySceneryId(int sceneryId) {
		return choppables.get(sceneryId);
	}
}
