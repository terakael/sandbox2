package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import database.dto.UndeadArmyWavesDto;
import lombok.Getter;
import utils.Utils;

public class UndeadArmyWavesDao {
	@Getter
	private static Map<Integer, Set<UndeadArmyWavesDto>> waves = new HashMap<>();

	public static void setupCaches(final int topLeftTileId) {
		setupWaves(topLeftTileId);
	}

	private static void setupWaves(final int topLeftTileId) {
		final String query = "select wave, npc_id, offset_x, offset_y from undead_army_waves";
		DbConnection.load(query, rs -> {
			waves.putIfAbsent(rs.getInt("wave"), new HashSet<>());
			waves.get(rs.getInt("wave"))
					.add(new UndeadArmyWavesDto(
							rs.getInt("wave"),
							rs.getInt("npc_id"),
							Utils.getTileIdFromRelativeXY(topLeftTileId, rs.getInt("offset_x"),
									rs.getInt("offset_y"))));
		});
	}

	public static Set<UndeadArmyWavesDto> getWave(int wave) {
		if (!waves.containsKey(wave))
			return new HashSet<>();
		return waves.get(wave);
	}
}
