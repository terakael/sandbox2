package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;

public class HousingTilesDao {
	private static Map<Integer, Map<Integer, Integer>> housingTileInstances = null; // floor, <tileId, houseId>
	private static Map<Integer, Map<Integer, Set<Integer>>> walkableTilesByHouseId = null; // floor, <houseId, <tileIds>>
	private static Map<Integer, Integer> houseOwnerIds = null; // houseId, playerid
	
	public static void setupCaches() {
		housingTileInstances = new HashMap<>();
		walkableTilesByHouseId = new HashMap<>();
		DbConnection.load("select floor, tile_id, house_id from housing_tiles", rs -> {
			// quick lookup for houseIds based on floor/tileId
			housingTileInstances.putIfAbsent(rs.getInt("floor"), new HashMap<>());
			housingTileInstances.get(rs.getInt("floor")).put(rs.getInt("tile_id"), rs.getInt("house_id"));
			
			// quick lookup for tileIds based on floor/houseId.  Note we only want walkable tiles.
			walkableTilesByHouseId.putIfAbsent(rs.getInt("floor"), new HashMap<>());
			walkableTilesByHouseId.get(rs.getInt("floor")).putIfAbsent(rs.getInt("house_id"), new HashSet<>());
			walkableTilesByHouseId.get(rs.getInt("floor")).get(rs.getInt("house_id")).add(rs.getInt("tile_id"));
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
		if (walkableTilesByHouseId.containsKey(floor) && walkableTilesByHouseId.get(floor).containsKey(houseId))
			return walkableTilesByHouseId.get(floor).get(houseId);
		return null;
	}
}
