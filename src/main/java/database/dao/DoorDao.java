package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import database.DbConnection;
import database.dto.DoorDto;
import database.dto.LockedDoorDto;
import processing.PathFinder;
import processing.managers.LockedDoorManager;

public class DoorDao {
	private static Map<Integer, DoorDto> doors = new HashMap<>(); // <scenery_id, dto>
	@Getter private static HashMap<Integer, HashMap<Integer, HashSet<Integer>>> doorInstances = null; // <floor, <scenery_id, tile_id>>
	
	private static Map<Integer, Map<Integer, Boolean>> doorStatuses = null; // open = true, closed = false
	
	public static void setupCaches() {
		setupDoorCache();
		setupDoorInstanceCache();
		setupLockedDoorCache();
	}
	
	public static DoorDto getDoorDtoByTileId(int floor, int tileId) {
		if (!doorInstances.containsKey(floor))
			return null;
		
		for (HashMap.Entry<Integer, HashSet<Integer>> instances : doorInstances.get(floor).entrySet()) {
			if (instances.getValue().contains(tileId))
				return doors.get(instances.getKey());
		}
		return null;
	}
	
	public static int getDoorImpassableByTileId(int floor, int tileId) {
		if (!doorStatuses.containsKey(floor))
			return 0;
		
		if (!doorStatuses.get(floor).containsKey(tileId))
			return 0;
		
		// iterating through all the sceneryIds is a bit slow...
		for (HashMap.Entry<Integer, HashSet<Integer>> entry : doorInstances.get(floor).entrySet()) {
			if (entry.getValue().contains(tileId)) {
				// found our scenery_id for the door on this tile
				return doorStatuses.get(floor).get(tileId) 
						? doors.get(entry.getKey()).getOpenImpassable() 
						: doors.get(entry.getKey()).getClosedImpassable();
			}
		}
		
		return 0;
	}
	
	public static void toggleDoor(int floor, int tileId) {
		if (!doorStatuses.containsKey(floor))
			return;
		
		if (!doorStatuses.get(floor).containsKey(tileId))
			return;
		
		doorStatuses.get(floor).put(tileId, !doorStatuses.get(floor).get(tileId));
	}
	
	public static boolean doorIsOpen(int floor, int tileId) {
		if (!doorStatuses.containsKey(floor))
			return true;
		
		if (!doorStatuses.get(floor).containsKey(tileId))
			return true;
		
		return doorStatuses.get(floor).get(tileId);
	}
	
	public static Set<Integer> getOpenDoorTileIds(int floor) {
		Set<Integer> openDoorTileIds = new HashSet<>();
		if (!doorStatuses.containsKey(floor))
			return openDoorTileIds;
		
		return doorStatuses.get(floor).entrySet().stream()
				.filter(map -> map.getValue())
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}
	
	private static void setupDoorCache() {
		DbConnection.load("select scenery_id, open_impassable, closed_impassable from doors", 
				rs -> doors.put(rs.getInt("scenery_id"), new DoorDto(rs.getInt("scenery_id"), rs.getInt("open_impassable"), rs.getInt("closed_impassable"))));
	}
	
	private static void setupDoorInstanceCache() {
		doorInstances = new HashMap<>();
		doorStatuses = new HashMap<>();
		
		if (doors == null || doors.isEmpty())
			return;
		
		for (Map.Entry<Integer, DoorDto> entry : doors.entrySet()) {
			final String query = "select floor, tile_id from room_scenery where scenery_id = ?";
			
			final int sceneryId = entry.getValue().getSceneryId();
			DbConnection.load(query, rs -> {
				doorInstances.putIfAbsent(rs.getInt("floor"), new HashMap<>());
				doorInstances.get(rs.getInt("floor")).putIfAbsent(sceneryId, new HashSet<>());
				doorInstances.get(rs.getInt("floor")).get(sceneryId).add(rs.getInt("tile_id"));
				
				// set all the door statuses while we're here
				doorStatuses.putIfAbsent(rs.getInt("floor"), new HashMap<>());
				doorStatuses.get(rs.getInt("floor")).put(rs.getInt("tile_id"), false); // all doors are closed on server startup.
			}, sceneryId);
		}
	}
	
	private static void setupLockedDoorCache() {
		final String query = "select floor, tile_id, unlock_item_id, destroy_on_use from locked_door_instances";
		
		Map<Integer, Map<Integer, LockedDoorDto>> lockedDoorInstances = new HashMap<>();
		DbConnection.load(query, rs -> {
			if (!lockedDoorInstances.containsKey(rs.getInt("floor")))
				lockedDoorInstances.put(rs.getInt("floor"), new HashMap<>());
			lockedDoorInstances.get(rs.getInt("floor")).put(rs.getInt("tile_id"), new LockedDoorDto(rs.getInt("floor"), rs.getInt("tile_id"), rs.getInt("unlock_item_id"), rs.getBoolean("destroy_on_use")));
		});
		
		// all doors on the edge of player house land are locked to all players except the owner; add all the doors to this list
		final Map<Integer, Set<Integer>> housingTiles = new HashMap<>();
		DbConnection.load("select floor, tile_id from housing_tiles", rs -> {
			housingTiles.putIfAbsent(rs.getInt("floor"), new HashSet<>());
			housingTiles.get(rs.getInt("floor")).add(rs.getInt("tile_id"));
		});
		
		doorInstances.forEach((floor, instances) -> {
			if (housingTiles.containsKey(floor)) {
				Set<Integer> edgeTiles = housingTiles.get(floor).stream()
						.filter(x -> {
							// if the tile is surrounded by other housing tiles, filter it out.
							// technically this checks for any housing tile, and not just the current house.
							// seems like an impossiblity to have a door from one house into another house though.
							return !housingTiles.get(floor).contains(x - 1) || 
									!housingTiles.get(floor).contains(x + 1) ||
									!housingTiles.get(floor).contains(x - PathFinder.LENGTH) ||
									!housingTiles.get(floor).contains(x + PathFinder.LENGTH);
						})
						.collect(Collectors.toSet());
				
				Set<Integer> doorTileIds = instances.values().stream()
					.flatMap(Set::stream)
					.collect(Collectors.toSet());
				doorTileIds.retainAll(edgeTiles); // retain only the door tileIds that are on a housing edge tileId
				
				lockedDoorInstances.get(floor).putAll(doorTileIds.stream()
						.collect(Collectors.toMap(Function.identity(), tileId -> new LockedDoorDto(floor, tileId, 0, false))));
			}
		});
		
		LockedDoorManager.setLockedDoorInstances(lockedDoorInstances);
	}
}
