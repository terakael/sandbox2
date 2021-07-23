package database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import database.DbConnection;
import database.dto.MineableDto;

public class MineableDao {
	private MineableDao() {}
	
	private static List<MineableDto> mineables = new ArrayList<>();
	@Getter private static HashMap<Integer, HashMap<Integer, HashSet<Integer>>> mineableInstances = null;
	
	public static void setupCaches() {
		cacheMineables();
		cacheMineableInstances();
	}
	
	public static MineableDto getMineableDtoByTileId(int floor, int tileId) {
		if (!mineableInstances.containsKey(floor))
			return null;
		
		for (HashMap.Entry<Integer, HashSet<Integer>> instances : mineableInstances.get(floor).entrySet()) {
			if (instances.getValue().contains(tileId)) {
				for (MineableDto dto : mineables) {
					if (dto.getSceneryId() == instances.getKey())
						return dto;
				}
			}
		}
		return null;
	}
	
	private static void cacheMineableInstances() {
		mineableInstances = new HashMap<>();
		
		// run through every rock's scenery_id and pull out all the instance tiles
		for (MineableDto dto : mineables) {
			final String query = "select floor, tile_id from room_scenery where scenery_id = ?";
			DbConnection.load(query, rs -> {
				if (!mineableInstances.containsKey(rs.getInt("floor")))
					mineableInstances.put(rs.getInt("floor"), new HashMap<>());
				
				if (!mineableInstances.get(rs.getInt("floor")).containsKey(dto.getSceneryId()))
					mineableInstances.get(rs.getInt("floor")).put(dto.getSceneryId(), new HashSet<>());
				
				mineableInstances.get(rs.getInt("floor")).get(dto.getSceneryId()).add(rs.getInt("tile_id"));
			}, dto.getSceneryId());
		}
	}
	
	private static void cacheMineables() {
		final String query = "select scenery_id, level, exp, item_id, respawn_ticks, gold_chance from mineable";
		DbConnection.load(query, rs -> {
			mineables.add(new MineableDto(rs.getInt("scenery_id"), rs.getInt("level"), rs.getInt("exp"), rs.getInt("item_id"), rs.getInt("respawn_ticks"), rs.getInt("gold_chance")));
		});
	}
	
	public static Set<MineableDto> getAllMineables() {
		return new HashSet<>(mineables);
	}
}
