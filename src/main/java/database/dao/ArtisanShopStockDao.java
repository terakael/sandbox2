package database.dao;

import java.util.LinkedHashMap;
import java.util.Map;

import database.DbConnection;
import lombok.Getter;

public class ArtisanShopStockDao {
	@Getter private static Map<Integer, Integer> shopStock = new LinkedHashMap<>(); // itemId, numPoints 
	
	public static void setupCaches() {
		DbConnection.load("select * from artisan_shop_stock", rs -> {
			shopStock.put(rs.getInt("item_id"), rs.getInt("num_points"));
		});
	}
}
