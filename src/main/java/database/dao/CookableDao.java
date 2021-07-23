package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import database.dto.CookableDto;

public class CookableDao {
	private static Map<Integer, CookableDto> cookables = new HashMap<>();// raw_item_id, dto
	
	public static void cacheCookables() {
		DbConnection.load("select raw_item_id, cooked_item_id, level, exp, burnt_item_id from cookable", 
				rs -> cookables.put(rs.getInt("raw_item_id"), new CookableDto(rs.getInt("raw_item_id"), rs.getInt("cooked_item_id"), rs.getInt("level"), rs.getInt("exp"), rs.getInt("burnt_item_id"))));
	}
	
	public static CookableDto getCookable(int itemId) {
		if (cookables.containsKey(itemId))
			return cookables.get(itemId);
		return null;
	}
	
	public static Set<CookableDto> getAllCookables() {
		return new HashSet<>(cookables.values());
	}
}
