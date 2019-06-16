package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;

public class ShopDao {
	// hashmap of <OwnerId, ShopId>
	@Getter private static HashMap<Integer, Integer> shopIdsByOwnerId = new HashMap<>();
	private static HashMap<Integer, String> shopNames = new HashMap<>();
	
	public static void setupCaches() {
		cacheShopOwnerMap();
		cacheShopNames();
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
	
	private static void cacheShopNames() {
		final String query = "select id, name from shops";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					shopNames.put(rs.getInt("id"), rs.getString("name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static String getShopNameById(int shopId) {
		if (shopNames.containsKey(shopId))
			return shopNames.get(shopId);
		return null;
	}
	
	public static Integer getShopIdByOwnerId(int ownerId) {
		if (shopIdsByOwnerId.containsKey(ownerId))
			return shopIdsByOwnerId.get(ownerId);
		return null;
	}
	
	public static ArrayList<ShopDto> getShopStockById(int shopId) {
		final String query = "select shop_id, item_id, default_stock from shop_stock where shop_id = ? order by item_id";
		
		ArrayList<ShopDto> list = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, shopId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					list.add(new ShopDto(rs.getInt("item_id"), rs.getInt("default_stock"), rs.getInt("default_stock"), ItemDao.getItem(rs.getInt("item_id")).getPrice()));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}
}
