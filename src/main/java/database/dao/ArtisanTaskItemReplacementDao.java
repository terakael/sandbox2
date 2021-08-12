package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;
import lombok.Getter;

public class ArtisanTaskItemReplacementDao {
	@Getter private static Map<Integer, Integer> replacementMap = new HashMap<>();
	
	public static void setupCaches() {
		DbConnection.load("select * from artisan_task_item_replacements", rs -> {
			replacementMap.put(rs.getInt("actual_item_id"), rs.getInt("replacement_item_id"));
		});
	}
}
