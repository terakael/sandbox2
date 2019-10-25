package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class UseItemOnItemDao {
	private static Map<String, UseItemOnItemDto> map; 
	
	public static void cacheData() {
		final String query = "select * from use_item_on_item";
		
		map = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery()
		) {
			while (rs.next()) {
				String id = String.format("%d_%d", rs.getInt("src_id"), rs.getInt("dest_id"));
				map.put(id, new UseItemOnItemDto(
								rs.getInt("src_id"), 
								rs.getInt("dest_id"), 
								rs.getInt("src_required_count"), 
								rs.getInt("resulting_item_id"), 
								rs.getInt("resulting_item_count"), 
								rs.getBoolean("keep_src_item")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static UseItemOnItemDto getEntryBySrcIdDestId(int src, int dest) {
		String id = String.format("%d_%d", src, dest);
		if (map.containsKey(id))
			return map.get(id);
		return null;
	}
}
