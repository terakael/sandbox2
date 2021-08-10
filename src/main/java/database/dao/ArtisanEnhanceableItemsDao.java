package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;
import database.dto.ArtisanEnhanceableItemsDto;
import lombok.Getter;

public class ArtisanEnhanceableItemsDao {
	@Getter private static Map<Integer, ArtisanEnhanceableItemsDto> enhanceableItems = new HashMap<>(); // itemId, enhancedItemId
	
	public static void setupCaches() {
		DbConnection.load("select * from artisan_enhanceable_items", rs -> {
			enhanceableItems.put(rs.getInt("item_id"), 
					new ArtisanEnhanceableItemsDto(rs.getInt("item_id"), rs.getInt("enhanced_item_id"), rs.getInt("num_points")));
		});
	}
}
