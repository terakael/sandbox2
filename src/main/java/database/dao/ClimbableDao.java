package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;

public class ClimbableDao {
	private static Map<Integer, Integer> climbables = new HashMap<>();
	
	public static void setupCaches() {
		cacheClimbables();
	}
	
	private static void cacheClimbables() {
		DbConnection.load("select scenery_id, relative_floors from climbable", 
				rs -> climbables.put(rs.getInt("scenery_id"), rs.getInt("relative_floors")));
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
