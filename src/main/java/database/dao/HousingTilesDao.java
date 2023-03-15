package database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import database.DbConnection;

public class HousingTilesDao {
	private static Map<Integer, Map<Integer, Integer>> housingTileInstances = null; // floor, <tileId, houseId>
	private static Map<Integer, Map<Integer, Set<Integer>>> walkableTilesByHouseId = null; // houseId, <floor, <tileIds>>
	private static Map<Integer, Integer> houseOwnerIds = null; // houseId, playerid
	
	public static void setupCaches() {
		housingTileInstances = new HashMap<>();
		walkableTilesByHouseId = new HashMap<>();
		DbConnection.load("select floor, tile_id, house_id from housing_tiles", rs -> {
			// quick lookup for houseIds based on floor/tileId
			housingTileInstances.putIfAbsent(rs.getInt("floor"), new HashMap<>());
			housingTileInstances.get(rs.getInt("floor")).put(rs.getInt("tile_id"), rs.getInt("house_id"));
			
			// quick lookup for tileIds based on floor/houseId.  Note we only want walkable tiles.
			walkableTilesByHouseId.putIfAbsent(rs.getInt("house_id"), new HashMap<>());
			walkableTilesByHouseId.get(rs.getInt("house_id")).putIfAbsent(rs.getInt("floor"), new HashSet<>());
			walkableTilesByHouseId.get(rs.getInt("house_id")).get(rs.getInt("floor")).add(rs.getInt("tile_id"));
		});
		
		houseOwnerIds = new HashMap<>();
		DbConnection.load("select id, house_id from player where house_id is not null", rs -> {
			houseOwnerIds.put(rs.getInt("house_id"), rs.getInt("id"));
		});
	}
	
	public static int getOwningPlayerId(int floor, int tileId) {
		int houseId = getHouseIdFromFloorAndTileId(floor, tileId);
		if (!houseOwnerIds.containsKey(houseId))
			return -1;
		return houseOwnerIds.get(houseId);
	}
	
	public static int getHouseIdFromFloorAndTileId(int floor, int tileId) {
		if (!housingTileInstances.containsKey(floor) || !housingTileInstances.get(floor).containsKey(tileId))
			return -1;
		return housingTileInstances.get(floor).get(tileId);
	}
	
	public static Set<Integer> getWalkableTilesByHouseId(int houseId, int floor) {
		if (walkableTilesByHouseId.containsKey(houseId) && walkableTilesByHouseId.get(houseId).containsKey(floor))
			return walkableTilesByHouseId.get(houseId).get(floor);
		return null;
	}
	
	public static int getHouseIdByPlayerId(int playerId) {
		return houseOwnerIds.entrySet().stream()
				.filter(entry -> entry.getValue() == playerId)
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(-1);
	}
	
	public static int[] getRandomWalkableTileByPlayerId(int playerId) {
		final int houseId = getHouseIdByPlayerId(playerId);
		if (houseId == -1)
			return null;
		
		// this is a mess, there must be a better way
		Map<Integer, Set<Integer>> allWalkableTiles = getAllWalkableTilesByHouseId(houseId);
		List<Integer> floorList = new ArrayList<>(allWalkableTiles.keySet());
		int randomFloor = floorList.get(new Random().nextInt(floorList.size()));
		List<Integer> tileList = new ArrayList<>(allWalkableTiles.get(randomFloor));
		int randomTile = tileList.get(new Random().nextInt(tileList.size()));
		
		return new int[] {randomFloor, randomTile};
	}
	
	public static Map<Integer, Set<Integer>> getAllWalkableTilesByHouseId(int houseId) {
		return walkableTilesByHouseId.get(houseId);
	}
}
