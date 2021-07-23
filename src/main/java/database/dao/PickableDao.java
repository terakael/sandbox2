package database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import database.dto.PickableDto;

public class PickableDao {
	private static ArrayList<PickableDto> pickables = new ArrayList<>();
	private static Map<Integer, Map<Integer, Set<Integer>>> pickableInstances = new HashMap<>(); // floor, <sceneryId, <tileIds>>
	
	public static void setupCaches() {
		cachePickables();
		cachePickableInstances();
	}
	
	private static void cachePickables() {
		final String query = "select scenery_id, item_id, respawn_ticks, scenery_attributes from pickable";
		DbConnection.load(query, rs -> 
			pickables.add(new PickableDto(rs.getInt("scenery_id"), rs.getInt("item_id"), rs.getInt("respawn_ticks"), rs.getInt("scenery_attributes"))));
	}
	
	private static void cachePickableInstances() {
		pickableInstances = new HashMap<>();
		
		// run through every rock's scenery_id and pull out all the instance tiles
		for (PickableDto dto : pickables) {
			final String query = "select floor, tile_id from room_scenery where scenery_id = ?";
			DbConnection.load(query, rs -> {
				pickableInstances.putIfAbsent(rs.getInt("floor"), new HashMap<>());
				pickableInstances.get(rs.getInt("floor")).putIfAbsent(dto.getSceneryId(), new HashSet<>());
				pickableInstances.get(rs.getInt("floor")).get(dto.getSceneryId()).add(rs.getInt("tile_id"));
			}, dto.getSceneryId());
		}
	}
	
	public static PickableDto getPickableByTileId(int floor, int tileId) {
		if (!pickableInstances.containsKey(floor))
			return null;
		
		for (Map.Entry<Integer, Set<Integer>> instances : pickableInstances.get(floor).entrySet()) {
			if (instances.getValue().contains(tileId)) {
				for (PickableDto dto : pickables) {
					if (dto.getSceneryId() == instances.getKey())
						return dto;
				}
			}
		}
		return null;
	}
	
	public static PickableDto getPickableBySceneryId(int sceneryId) {
		return pickables.stream().filter(e -> e.getSceneryId() == sceneryId).findFirst().orElse(null);
	}
	
	public static Map<Integer, Set<Integer>> getPickablesByFloor(int floor) {
		if (!pickableInstances.containsKey(floor))
			return new HashMap<>();
		return pickableInstances.get(floor);
	}
	
	public static boolean isItemPickable(int itemId) {
		return pickables.stream().filter(e -> e.getItemId() == itemId).findFirst().isPresent();
	}
	
	public static Set<PickableDto> getAllPickables() {
		return new HashSet<>(pickables);
	}
}
