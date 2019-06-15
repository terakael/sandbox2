package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class ShopDao {
	// hashmap of <OwnerId, ShopId>
	private static HashMap<Integer, Integer> shopIdsByOwnerId = new HashMap<>();
	
	public static void setupCaches() {
		cacheShopOwnerMap();
	}
	
	private static void cacheShopOwnerMap() {
		final String query = "select shop_id, owner_id from shop_owners";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					shopIdsByOwnerId.put(rs.getInt("owner_id"), rs.getInt("shop_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static Integer getShopIdByOwnerId(int ownerId) {
		if (shopIdsByOwnerId.containsKey(ownerId))
			return shopIdsByOwnerId.get(ownerId);
		return null;
	}
	
	public static HashMap<Integer, ShopDto> getShopStockByOwnerId(int ownerId) {
		final String query = 
				"select shop_stock.shop_id, shop_stock.item_id, shop_stock.default_stock, shop_stock.price from shop_owners" + 
				" inner join shop_stock on shop_stock.shop_id = shop_owners.shop_id" + 
				" where shop_owners.owner_id = ?" + 
				" order by item_id";
		
		HashMap<Integer, ShopDto> dtos = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, ownerId);
			try (ResultSet rs = ps.executeQuery()) {
				int slot = 0;
				while (rs.next()) {
					dtos.put(slot, new ShopDto(rs.getInt("item_id"), rs.getInt("default_stock"), rs.getInt("default_stock"), slot, rs.getInt("price")));
					++slot;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return dtos;
	}

	public static ArrayList<ShopDto> getShopStockById(int shopId) {
		final String query = "select shop_id, item_id, default_stock, price from shop_stock where shop_id = ? order by item_id";
		
		ArrayList<ShopDto> list = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, shopId);
			try (ResultSet rs = ps.executeQuery()) {
				int slot = 0;
				while (rs.next())
					list.add(new ShopDto(rs.getInt("item_id"), rs.getInt("default_stock"), rs.getInt("default_stock"), slot++, rs.getInt("price")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}
}
