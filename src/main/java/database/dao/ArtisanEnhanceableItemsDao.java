package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;

public class ArtisanEnhanceableItemsDao {
	private static Map<Integer, Integer> enhanceableItems = new HashMap<>(); // itemId, enhancedItemId
	
	public static void setupCaches() {
		DbConnection.load("select * from artisan_enhanceable_items", rs -> {
			enhanceableItems.put(rs.getInt("item_id"), rs.getInt("enhanced_item_id"));
		});
	}
}
