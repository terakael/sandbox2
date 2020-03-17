package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import main.database.GroundTextureDao;
import main.database.ItemDao;
import main.database.RespawnableDao;
import main.database.RespawnableDto;
import main.processing.PathFinder;
import main.processing.RoomGroundItemManager;
import main.processing.RoomGroundItemManager.GroundItem;
import main.processing.RoomGroundItemManager.RespawnableGroundItem;
import main.types.ItemAttributes;

interface IGenericFunc {
	void handle(RoomGroundItemManager groundItemManager);
}

public class GroundItemManager {
	private static HashMap<Integer, RoomGroundItemManager> groundItemManagers = new HashMap<>();
	
	public static void initialize() {		
		for (int roomId : GroundTextureDao.getDistinctRoomIds()) {
			RoomGroundItemManager groundItemManager = new RoomGroundItemManager(roomId);
			groundItemManager.setupRespawnables();
			groundItemManagers.put(roomId, groundItemManager);
		}
	}
	
	private static void allGroundItemManagers(IGenericFunc func) {
		for (Map.Entry<Integer, RoomGroundItemManager> entry : groundItemManagers.entrySet()) {
			func.handle(entry.getValue());
		}
	}
	
	public static boolean itemIsRespawnable(int roomId, int tileId, int itemId) {
		if (!groundItemManagers.containsKey(roomId))
			return false;
		
		return groundItemManagers.get(roomId).itemIsRespawnable(tileId, itemId);
	}
	
	public static void process() {
		allGroundItemManagers(RoomGroundItemManager::process);
	}

	public static void add(int roomId, int playerId, int itemId, int tileId, int count, int charges) {
		if (!groundItemManagers.containsKey(roomId))
			return;
		
		groundItemManagers.get(roomId).add(playerId, itemId, tileId, count, charges);
	}

	public static void remove(int roomId, int playerId, int tileId, int itemId, int count, int charges) {
		if (!groundItemManagers.containsKey(roomId))
			return;
		
		groundItemManagers.get(roomId).remove(playerId, tileId, itemId, count, charges);
	}
	
	public static boolean itemIsOnGround(int roomId, int playerId, int itemId) {		
		if (!groundItemManagers.containsKey(roomId))
			return false;
		
		return groundItemManagers.get(roomId).itemIsOnGround(playerId, itemId);
	}
	
	public static HashMap<Integer, ArrayList<Integer>> getItemIdsNearTile(int roomId, int playerId, int tileId, int proximity) {
		if (!groundItemManagers.containsKey(roomId))
			return new HashMap<>();
		
		return groundItemManagers.get(roomId).getItemIdsNearTile(playerId, tileId, proximity);
	}
	
	public static GroundItem getItemAtTileId(int roomId, int playerId, int itemId, int tileId) {
		if (!groundItemManagers.containsKey(roomId))
			return null;
		
		return groundItemManagers.get(roomId).getItemAtTileId(playerId, itemId, tileId);
	}
}
