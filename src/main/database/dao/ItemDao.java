package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.database.DbConnection;
import main.database.dto.ItemChargesDto;
import main.database.dto.ItemDto;
import main.types.ItemAttributes;

public class ItemDao {
	private ItemDao() {};
	
	private static HashMap<Integer, ItemDto> itemMap = new HashMap<>();
	private static HashMap<Integer, ItemChargesDto> itemMaxCharges = new HashMap<>();
	
	public static String getNameFromId(int id) {
		if (itemMap.containsKey(id))
			return itemMap.get(id).getName();
		return null;
	}
	
	public static Integer getIdFromName(String name) {
		for (HashMap.Entry<Integer, ItemDto> entry : itemMap.entrySet()) {
			if (entry.getValue().getName().equals(name))
				return entry.getKey();
		}
		return null;
	}
	
	public static void setupCaches() {
		populateItemCache();
		populateMaxChargesCache();
	}
	
	private static void populateItemCache() {
		final String query = "select id, name, sprite_frame_id, leftclick_option, other_options, attributes, price, shiftclick_option from items";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					itemMap.put(rs.getInt("id"), new ItemDto(rs.getInt("id"), 
							rs.getString("name"), 
							rs.getInt("sprite_frame_id"), 
							rs.getInt("leftclick_option"), 
							rs.getInt("other_options"), 
							rs.getInt("attributes"),
							rs.getInt("price"),
							rs.getInt("shiftclick_option")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void populateMaxChargesCache() {
		final String query = "select item_id, max_charges, degraded_item_id from item_charges";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					itemMaxCharges.put(rs.getInt("item_id"), new ItemChargesDto(rs.getInt("item_id"), rs.getInt("max_charges"), rs.getInt("degraded_item_id")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static int getMaxCharges(int itemId) {
		if (itemMaxCharges.containsKey(itemId))
			return itemMaxCharges.get(itemId).getMaxCharges();
		return 0;
	}
	
	public static int getDegradedItemId(int itemId) {
		if (itemMaxCharges.containsKey(itemId))
			return itemMaxCharges.get(itemId).getDegradedItemId();
		return 0;
	}

	public static List<ItemDto> getAllItems() {
		return new ArrayList<>(itemMap.values());
	}
	
	public static HashMap<Integer, String> getExamineMap() {
		final String query = "select id, description from items";
		HashMap<Integer, String> examineMap = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					examineMap.put(rs.getInt("id"), rs.getString("description"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return examineMap;
	}
	
	public static boolean itemHasAttribute(int itemId, ItemAttributes attribute) {
		if (itemMap.containsKey(itemId))
			return (itemMap.get(itemId).getAttributes() & attribute.getValue()) == attribute.getValue();
		return false;
	}
	
	public static ItemDto getItem(int itemId) {
		if (itemMap.containsKey(itemId))
			return itemMap.get(itemId);
		return null;
	}
}
