package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemDao {
	private ItemDao() {};
	
	private static HashMap<Integer, String> itemIdNameMap = new HashMap<>();
	
	public static String getNameFromId(int id) {
		if (itemIdNameMap.containsKey(id))
			return itemIdNameMap.get(id);
		return null;
	}
	
	public static void setupCaches() {
		populateIdNameMap();
	}
	
	private static void populateIdNameMap() {
		final String query = "select id, name from items";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					itemIdNameMap.put(rs.getInt("id"), rs.getString("name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static ItemDto getItemById(int id) {
		final String query = "select id, name, description, sprite_frame_id, leftclick_option, other_options from items where id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new ItemDto(rs.getInt("id"), rs.getString("name"), rs.getString("description"), rs.getInt("sprite_frame_id"), rs.getInt("leftclick_option"), rs.getInt("other_options"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static List<ItemDto> getAllItems() {
		final String query = "select id, name, description, sprite_frame_id, leftclick_option, other_options from items";
		List<ItemDto> items = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					items.add(new ItemDto(rs.getInt("id"), rs.getString("name"), rs.getString("description"), rs.getInt("sprite_frame_id"), rs.getInt("leftclick_option"), rs.getInt("other_options")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return items;
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
}
