package main.database.dao;

import java.util.HashMap;
import java.util.Map;

import main.database.DbConnection;
import main.database.dto.ReinforcementBonusesDto;

public class ReinforcementBonusesDao {
	private static Map<Integer, ReinforcementBonusesDto> reinforcementBonuses = new HashMap<>();
	
	public static void setupCaches() {
		cacheReinforcementBonuses();
	}
	
	private static void cacheReinforcementBonuses() {
		final String query = "select reinforcement_id, proc_chance, soak_pct from reinforcement_bonuses";
		DbConnection.load(query, rs -> {
			reinforcementBonuses.put(rs.getInt("reinforcement_id"), 
					new ReinforcementBonusesDto(rs.getInt("reinforcement_id"), rs.getInt("proc_chance"), rs.getFloat("soak_pct")));
		});
	}
	
	public static ReinforcementBonusesDto getReinforcementBonusesById(int id) {
		return reinforcementBonuses.get(id);
	}
}
