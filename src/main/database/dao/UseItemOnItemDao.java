package main.database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import main.database.DbConnection;
import main.database.dto.UseItemOnItemDto;

public class UseItemOnItemDao {
	private static Map<String, UseItemOnItemDto> map = new HashMap<>(); 
	
	public static void cacheData() {
		DbConnection.load("select * from use_item_on_item", rs -> {
			String id = String.format("%d_%d", rs.getInt("src_id"), rs.getInt("dest_id"));
			map.put(id, new UseItemOnItemDto(
							rs.getInt("src_id"), 
							rs.getInt("dest_id"), 
							rs.getInt("src_required_count"), 
							rs.getInt("resulting_item_id"), 
							rs.getInt("resulting_item_count"), 
							rs.getBoolean("keep_src_item")));
		});
	}
	
	public static UseItemOnItemDto getEntryBySrcIdDestId(int src, int dest) {
		String id = String.format("%d_%d", src, dest);
		if (map.containsKey(id))
			return map.get(id);
		return null;
	}
	public static Set<UseItemOnItemDto> getAllDtos() {
		return new HashSet<>(map.values());
	}
}
