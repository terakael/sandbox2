package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import database.dto.UndeadArmyWavesDto;
import lombok.Getter;

public class UndeadArmyWavesDao {
	@Getter private static Map<Integer, Set<UndeadArmyWavesDto>> waves = new HashMap<>();
	
	public static void setupCaches() {
		setupWaves();
	}
	
	private static void setupWaves() {
		
		final String query = "select wave, npc_id, tile_id from undead_army_waves";
		DbConnection.load(query, rs -> {
			waves.putIfAbsent(rs.getInt("wave"), new HashSet<>());
			waves.get(rs.getInt("wave")).add(new UndeadArmyWavesDto(rs.getInt("wave"), rs.getInt("npc_id"), rs.getInt("tile_id")));
		});
	}
	
	public static Set<UndeadArmyWavesDto> getWave(int wave) {
		if (!waves.containsKey(wave))
			return new HashSet<>();
		return waves.get(wave);
	}
}
