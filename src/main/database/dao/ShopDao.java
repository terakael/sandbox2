package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;
import main.database.DbConnection;
import main.database.dto.ShopDto;
import main.database.dto.ShopItemDto;

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
	
	public static ArrayList<ShopDto> getShopsAndItems() {
		final String query = 
				"select shops.id, shops.name, shops.shop_type, owner_id from shops" + 
				" inner join shop_owners on shop_owners.shop_id = shops.id";
		ArrayList<ShopDto> dtos = new ArrayList<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int shopId = rs.getInt("id");
					ArrayList<ShopItemDto> shopItems = getShopStockById(shopId);
					
					dtos.add(new ShopDto(shopId, rs.getInt("owner_id"), rs.getString("name"), rs.getInt("shop_type"), shopItems));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return dtos;
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
	
	public static ArrayList<ShopItemDto> getShopStockById(int shopId) {
		final String query = "select shop_id, item_id, default_stock, respawn_ticks from shop_stock where shop_id = ? order by item_id";
		
		ArrayList<ShopItemDto> list = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, shopId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					list.add(new ShopItemDto(
								rs.getInt("item_id"), 
								rs.getInt("default_stock"), 
								rs.getInt("default_stock"), 
								ItemDao.getItem(rs.getInt("item_id")).getPrice(), 
								rs.getInt("respawn_ticks")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}
}
