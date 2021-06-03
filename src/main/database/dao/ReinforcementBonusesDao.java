package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import main.database.DbConnection;
import main.database.dto.ReinforcementBonusesDto;

public class ReinforcementBonusesDao {
	private static Map<Integer, ReinforcementBonusesDto> reinforcementBonuses;
	
	public static void setupCaches() {
		cacheReinforcementBonuses();
	}
	
	private static void cacheReinforcementBonuses() {
		reinforcementBonuses = new HashMap<>();
		
		final String query = "select reinforcement_id, proc_chance, soak_pct from reinforcement_bonuses";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					reinforcementBonuses.put(rs.getInt("reinforcement_id"), new ReinforcementBonusesDto(rs.getInt("reinforcement_id"), rs.getInt("proc_chance"), rs.getFloat("soak_pct")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static ReinforcementBonusesDto getReinforcementBonusesById(int id) {
		if (!reinforcementBonuses.containsKey(id))
			return null;
		return reinforcementBonuses.get(id);
	}
}
