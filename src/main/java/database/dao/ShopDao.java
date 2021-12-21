package database.dao;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;
import database.DbConnection;
import database.dto.ShopDto;
import database.dto.ShopItemDto;

public class ShopDao {
	// hashmap of <OwnerId, ShopId>
	@Getter private static HashMap<Integer, Integer> shopIdsByOwnerId = new HashMap<>();
	private static HashMap<Integer, String> shopNames = new HashMap<>();
	
	public static void setupCaches() {
		cacheShopOwnerMap();
		cacheShopNames();
	}
	
	private static void cacheShopOwnerMap() {
		DbConnection.load("select shop_id, owner_id from shop_owners", 
				rs -> shopIdsByOwnerId.put(rs.getInt("owner_id"), rs.getInt("shop_id")));
	}
	
	private static void cacheShopNames() {
		DbConnection.load("select id, name from shops", 
				rs -> shopNames.put(rs.getInt("id"), rs.getString("name")));
	}
	
	public static ArrayList<ShopDto> getShopsAndItems() {
		final String query = 
				"select shops.id, shops.name, shops.shop_type, owner_id from shops" + 
				" inner join shop_owners on shop_owners.shop_id = shops.id";
		ArrayList<ShopDto> dtos = new ArrayList<>();
		DbConnection.load(query, rs -> {
			int shopId = rs.getInt("id");
			ArrayList<ShopItemDto> shopItems = getShopStockById(shopId);
			
			dtos.add(new ShopDto(shopId, rs.getInt("owner_id"), rs.getString("name"), rs.getInt("shop_type"), shopItems));
		});
		
		return dtos;
	}
	
	public static String getShopNameById(int shopId) {
		return shopNames.get(shopId);
	}
	
	public static Integer getShopIdByOwnerId(int ownerId) {
		return shopIdsByOwnerId.get(ownerId);
	}
	
	public static ArrayList<ShopItemDto> getShopStockById(int shopId) {
		final String query = "select shop_id, item_id, default_stock, respawn_ticks from shop_stock where shop_id = ? order by item_id";
		
		ArrayList<ShopItemDto> list = new ArrayList<>();
		DbConnection.load(query, rs -> {
			list.add(new ShopItemDto(
					rs.getInt("item_id"), 
					rs.getInt("default_stock"), 
					rs.getInt("default_stock"), 
					ItemDao.getItem(rs.getInt("item_id")).getPrice(), 
					rs.getInt("respawn_ticks")));
		}, shopId);
		
		return list;
	}
}
