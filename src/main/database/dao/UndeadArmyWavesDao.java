package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import main.database.DbConnection;
import main.database.dto.UndeadArmyWavesDto;

public class UndeadArmyWavesDao {
	private static Map<Integer, Set<UndeadArmyWavesDto>> waves;
	
	public static void setupCaches() {
		setupWaves();
	}
	
	private static void setupWaves() {
		waves = new HashMap<>();
		
		final String query = "select wave, npc_id, tile_id from undead_army_waves";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					waves.putIfAbsent(rs.getInt("wave"), new HashSet<>());
					waves.get(rs.getInt("wave")).add(new UndeadArmyWavesDto(rs.getInt("wave"), rs.getInt("npc_id"), rs.getInt("tile_id")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static Set<UndeadArmyWavesDto> getWave(int wave) {
		if (!waves.containsKey(wave))
			return new HashSet<>();
		return waves.get(wave);
	}
}
