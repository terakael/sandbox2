package main.database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import main.database.DbConnection;
import main.database.dto.BrewableDto;

public class BrewableDao {
	private static HashMap<Integer, BrewableDto> brewables = new HashMap<>(); // potionId, dto
	
	public static void cacheBrewables() {
		DbConnection.load("select potion_id, level, exp from brewable", 
				rs -> brewables.put(rs.getInt("potion_id"), new BrewableDto(rs.getInt("potion_id"), rs.getInt("level"), rs.getInt("exp"))));
	}
	
	public static boolean isBrewable(int id) {
		return brewables.containsKey(id);
	}
	
	public static BrewableDto getBrewableById(int id) {
		if (brewables.containsKey(id))
			return brewables.get(id);
		return null;
	}
	
	public static int getRequiredLevelById(int id) {
		if (brewables.containsKey(id))
			return brewables.get(id).getLevel();
		return 0;
	}
	
	public static int getExpById(int id) {
		if (brewables.containsKey(id))
			return brewables.get(id).getExp();
		return 0;
	}
	
	public static Set<BrewableDto> getAllBrewables() {
		return new HashSet<>(brewables.values());
	}
}
