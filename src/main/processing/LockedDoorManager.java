package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Setter;
import main.database.LockedDoorDto;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.responses.OpenCloseResponse;
import main.responses.ResponseMaps;
import main.types.StorageTypes;

public class LockedDoorManager {
	@Setter private static Map<Integer, Map<Integer, LockedDoorDto>> lockedDoorInstances = new HashMap<>();
	// this is used to track the locked doors.
	// only one person can go through the door at a time and there's a two tick open-close cycle.
	// if a player is going through while it's closed, we want to open the door.
	// if a player is going through while it's open, we don't want to close it; just ignore any status change.
	private static Map<Integer, Map<Integer, Integer>> openLockedDoors = new HashMap<>(); // <floor, <tileid, remainingticks>>
	
	public static void process(ResponseMaps responseMaps) {
		for (Map.Entry<Integer, Map<Integer, Integer>> floorEntry : openLockedDoors.entrySet()) {
			floorEntry.getValue().replaceAll((k, v) -> v -= 1);
			
			Set<Integer> tileIds = floorEntry.getValue().entrySet().stream()
					.filter(e -> e.getValue() == 0)
					.map(e -> e.getKey())
					.collect(Collectors.toSet());
					
			// remove all the closed doors from the map
			floorEntry.getValue().keySet().removeIf(key -> tileIds.contains(key));
			
			for (int tileId : tileIds) {
				OpenCloseResponse resp = new OpenCloseResponse();
				resp.setTileId(tileId);
				responseMaps.addLocalResponse(floorEntry.getKey(), tileId, resp);
			}
		}
	}
	
	// returns true if the door has been opened, false if it was already open
	public static boolean openLockedDoor(int floor, int tileId) {
		if (!openLockedDoors.containsKey(floor))
			openLockedDoors.put(floor, new HashMap<>());
		return openLockedDoors.get(floor).put(tileId, 3) == null;// three ticks
	}
	
	public static Set<Integer> getOpenLockedDoorTileIds(int floor) {
		if (!openLockedDoors.containsKey(floor))
			return new HashSet<>();
		
		return openLockedDoors.get(floor).entrySet().stream()
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}
	
	public static boolean isLockedDoor(int floor, int tileId) {
		return getLockedDoor(floor, tileId) != null;
	}
	
	public static LockedDoorDto getLockedDoor(int floor, int tileId) {
		if (!lockedDoorInstances.containsKey(floor))
			return null;
		return lockedDoorInstances.get(floor).get(tileId);
	}
	
	public static int calculatePlayerNewTileId(int playerTileId, int doorTileId, int impassable) {
		if (playerTileId == doorTileId) {
			if ((impassable & 1) > 0) {
				return doorTileId - PathFinder.LENGTH;
			} else if ((impassable & 2) > 0) {
				return doorTileId - 1;
			} else if ((impassable & 4) > 0) {
				return doorTileId + 1;
			} else if ((impassable & 8) > 0) {
				return doorTileId + PathFinder.LENGTH;
			}
		}
		
		return doorTileId;
	}
	
	public static String playerMeetsDoorRequirements(Player player, LockedDoorDto lockedDoor) {
		// TODO this is where things like guild doors etc are handled
		List<Integer> invIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (lockedDoor.getUnlockItemId() != 0 && invIds.contains(lockedDoor.getUnlockItemId()))
			return "";
		
		// tybalt's house
		if (lockedDoor.getFloor() == 0 && lockedDoor.getTileId() == 873569404) {
			if (StatsDao.getCombatLevelByPlayerId(player.getId()) < 100) {
				return "you need to be 100 combat or higher to enter.";
			}
			return "";
		}
		
		return "the door is locked.";
	}
}
