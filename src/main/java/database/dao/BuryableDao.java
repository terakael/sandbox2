package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;

public class BuryableDao {
	private static Map<Integer, Integer> buryables = new HashMap<>();
	
	public static void setupCaches() {
		cacheBuryables();
	}
	
	private static void cacheBuryables() {
		DbConnection.load("select item_id, exp from buryable", 
				rs -> buryables.put(rs.getInt("item_id"), rs.getInt("exp")));
	}
	
	public static boolean isBuryable(int itemId) {
		return buryables.containsKey(itemId);
	}
	
	public static int getExpFromBuryable(int itemId) {
		if (!isBuryable(itemId))
			return 0;
		return buryables.get(itemId);
	}
}
