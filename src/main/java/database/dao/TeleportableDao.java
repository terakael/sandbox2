package database.dao;

import java.util.HashMap;

import database.DbConnection;
import database.dto.TeleportableDto;

public class TeleportableDao {
private static HashMap<Integer, TeleportableDto> teleportables = new HashMap<>(); // itemId, dto
	
	public static void setupCaches() {
		DbConnection.load("select item_id, floor, tile_id from teleportable", 
				rs -> teleportables.put(rs.getInt("item_id"), new TeleportableDto(rs.getInt("item_id"), rs.getInt("floor"), rs.getInt("tile_id"))));
	}
	
	public static boolean isTeleportable(int itemId) {
		return teleportables.containsKey(itemId);
	}
	
	public static TeleportableDto getTeleportableByItemId(int itemId) {
		return teleportables.get(itemId);
	}
}
