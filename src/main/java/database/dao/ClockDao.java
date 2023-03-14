package database.dao;

import java.util.HashSet;
import java.util.Set;

import database.DbConnection;

public class ClockDao {
	private static final Set<Integer> clockIds = new HashSet<>();
	public static void setupCaches() {
		DbConnection.load("select scenery_id from clocks", 
				rs -> clockIds.add(rs.getInt("scenery_id")));
	}
	
	public static boolean isClock(int sceneryId) {
		return clockIds.contains(sceneryId);
	}
}
