package database.dao;

import java.util.HashMap;

import database.DbConnection;

public class CatchableDao {
	private static HashMap<Integer, Integer> catchables = new HashMap<>();
	
	public static void cacheCatchables() {
		DbConnection.load("select npc_id, item_id from catchable", 
				rs -> catchables.put(rs.getInt("npc_id"), rs.getInt("item_id")));
	}
	
	public static boolean isCatchable(int npcId) {
		return catchables.containsKey(npcId);
	}
	
	public static int getCaughtItem(int npcId) {
		if (isCatchable(npcId))
			return catchables.get(npcId);
		return 0;
	}
}
