package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;

public class PetDao {
	private static Map<Integer, Integer> pets = new HashMap<>(); // itemId, npcId
	
	public static void setupCaches() {
		cachePets();
	}
	
	private static void cachePets() {
		DbConnection.load("select item_id, npc_id from pets", 
				rs -> pets.put(rs.getInt("item_id"), rs.getInt("npc_id")));
	}
	
	public static int getNpcIdFromItemId(int itemId) {
		if (pets.containsKey(itemId))
			return pets.get(itemId);
		return -1;
	}
	
	public static int getItemIdFromNpcId(int npcId) {
		for (Map.Entry<Integer, Integer> entry : pets.entrySet()) {
			if (entry.getValue() == npcId)
				return entry.getKey();
		}
		return -1;
	}
}
