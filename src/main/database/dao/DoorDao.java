package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import main.database.DbConnection;
import main.database.dto.DoorDto;
import main.database.dto.LockedDoorDto;
import main.processing.managers.LockedDoorManager;

public class DoorDao {
	private static Map<Integer, DoorDto> doors; // <scenery_id, dto>
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
		final String query = "select scenery_id, open_impassable, closed_impassable from doors";
		doors = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					doors.put(rs.getInt("scenery_id"), new DoorDto(rs.getInt("scenery_id"), rs.getInt("open_impassable"), rs.getInt("closed_impassable")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void setupDoorInstanceCache() {
		doorInstances = new HashMap<>();
		doorStatuses = new HashMap<>();
		
		if (doors == null || doors.isEmpty())
			return;
		
		for (Map.Entry<Integer, DoorDto> entry : doors.entrySet()) {
			final String query = "select floor, tile_id from room_scenery where scenery_id = ?";
			
			try (
				Connection connection = DbConnection.get();
				PreparedStatement ps = connection.prepareStatement(query);
			) {
				final int sceneryId = entry.getValue().getSceneryId();
				ps.setInt(1, sceneryId);
				
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						if (!doorInstances.containsKey(rs.getInt("floor")))
							doorInstances.put(rs.getInt("floor"), new HashMap<>());
						
						if (!doorInstances.get(rs.getInt("floor")).containsKey(sceneryId))
							doorInstances.get(rs.getInt("floor")).put(sceneryId, new HashSet<>());
						
						doorInstances.get(rs.getInt("floor")).get(sceneryId).add(rs.getInt("tile_id"));
						
						// set all the door statuses while we're here
						if (!doorStatuses.containsKey(rs.getInt("floor")))
							doorStatuses.put(rs.getInt("floor"), new HashMap<>());
						doorStatuses.get(rs.getInt("floor")).put(rs.getInt("tile_id"), false); // all doors are closed on server startup.
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void setupLockedDoorCache() {
		final String query = "select floor, tile_id, unlock_item_id, destroy_on_use from locked_door_instances";
		
		Map<Integer, Map<Integer, LockedDoorDto>> lockedDoorInstances = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (!lockedDoorInstances.containsKey(rs.getInt("floor")))
						lockedDoorInstances.put(rs.getInt("floor"), new HashMap<>());
					lockedDoorInstances.get(rs.getInt("floor")).put(rs.getInt("tile_id"), new LockedDoorDto(rs.getInt("floor"), rs.getInt("tile_id"), rs.getInt("unlock_item_id"), rs.getBoolean("destroy_on_use")));
				}
				
				LockedDoorManager.setLockedDoorInstances(lockedDoorInstances);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
