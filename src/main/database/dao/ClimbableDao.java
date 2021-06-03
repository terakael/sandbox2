package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import main.database.DbConnection;

public class ClimbableDao {
	private static Map<Integer, Integer> climbables;
	
	public static void setupCaches() {
		cacheClimbables();
	}
	
	private static void cacheClimbables() {
		final String query = "select scenery_id, relative_floors from climbable";
		
		climbables = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					climbables.put(rs.getInt("scenery_id"), rs.getInt("relative_floors"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isClimbable(int sceneryId) {
		return climbables.containsKey(sceneryId);
	}
	
	public static int getRelativeFloorsBySceneryId(int sceneryId) {
		if (!isClimbable(sceneryId))
			return 0;
		return climbables.get(sceneryId);
	}
}
