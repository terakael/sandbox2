package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.DbConnection;
import database.dto.SmithableDto;

public class SmithableDao {
	private static Map<Integer, SmithableDto> smithables = new HashMap<>();
	
	public static void setupCaches() {
		final String query = "select item_id, level, bar_id, required_bars from smithable";
		DbConnection.load(query, rs -> {
			smithables.put(rs.getInt("item_id"), new SmithableDto(
					rs.getInt("item_id"),
					rs.getInt("level"),
					rs.getInt("bar_id"),
					rs.getInt("required_bars")
			));
		});
	}
	
	public static List<SmithableDto> getAllItemsByBarId(int barId) {
		return smithables.values().stream().filter(e -> e.getBarId() == barId).collect(Collectors.toList());
	}
	
	public static SmithableDto getSmithableItemByItemId(int itemId) {
		return smithables.get(itemId);
	}
	
	public static Set<SmithableDto> getAllSmithables() {
		return new HashSet<>(smithables.values());
	}
}
