package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import main.database.dao.SceneryDao;
import main.database.dto.ConstructableDto;
import main.processing.scenery.constructable.BleedingTotemPole;
import main.processing.scenery.constructable.Constructable;
import main.processing.scenery.constructable.HolyTotemPole;
import main.processing.scenery.constructable.LargeStorageChest;
import main.processing.scenery.constructable.NaturesShrine;
import main.processing.scenery.constructable.SmallStorageChest;
import main.responses.ResponseMaps;
import main.responses.SceneryDespawnResponse;
import main.utils.Utils;

public class ConstructableManager {
	private static Map<Integer, Class<? extends Constructable>> constructables = new HashMap<>(); // sceneryId, constructable class
	private static Map<Integer, Map<Integer, Constructable>> constructableInstances = new HashMap<>(); // floor, <tileId, constructable>
	
	static {
		constructables.put(138, BleedingTotemPole.class);
		constructables.put(139, HolyTotemPole.class);
		constructables.put(140, NaturesShrine.class);
		constructables.put(141, SmallStorageChest.class);
		constructables.put(147, LargeStorageChest.class);
	}
	
	public static void process(int tickId, ResponseMaps responseMaps) {
		constructableInstances.forEach((floor, tileIdMap) -> {
			Set<Integer> tileIdsToRemove = new HashSet<>();
			tileIdMap.forEach((tileId, constructable) -> {
				constructable.process(tickId, responseMaps);
				if (constructable.getRemainingTicks() <= 0) {
					tileIdsToRemove.add(tileId);
					responseMaps.addLocalResponse(floor, tileId, new SceneryDespawnResponse(tileId));
				}
			});
			
			constructableInstances.get(floor).keySet().removeIf(e -> tileIdsToRemove.contains(e));
		});
	}
	
	public static int getConstructableIdByTileId(int floor, int tileId) {
		if (!constructableInstances.containsKey(floor))
			return -1;
		
		if (!constructableInstances.get(floor).containsKey(tileId))
			return -1;
		
		return constructableInstances.get(floor).get(tileId).getDto().getResultingSceneryId();
	}
	
	public static void add(int floor, int tileId, ConstructableDto constructable) {
		// if something already exists here then bail
		if (!PathFinder.tileIsValid(floor, tileId) || SceneryDao.getSceneryIdByTileId(floor, tileId) != -1 || getConstructableIdByTileId(floor, tileId) != -1)
			return;
		
		constructableInstances.putIfAbsent(floor, new HashMap<>());
		Constructable newConstructableInstance = null;
		if (constructables.containsKey(constructable.getResultingSceneryId())) {
			try {
				newConstructableInstance = constructables.get(constructable.getResultingSceneryId())
						.getDeclaredConstructor(int.class, int.class, ConstructableDto.class)
						.newInstance(floor, tileId, constructable);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		} else {
			newConstructableInstance = new Constructable(floor, tileId, constructable); // generic constructable, represents constructables that don't have special process logic
		}

		constructableInstances.get(floor).put(tileId, newConstructableInstance);
	}
	
	public static boolean constructableIsInRadius(int floor, int tileId, int constructableId, int radius) {
		if (!constructableInstances.containsKey(floor))
			return false;
		
		final Set<Integer> allLocalTiles = Utils.getLocalTiles(tileId, radius);
		final Set<Integer> localTilesWithConstructableInstance = constructableInstances.get(floor).keySet().stream()
				.filter(e -> allLocalTiles.contains(e))
				.collect(Collectors.toSet());
		
		for (int checkTileId : localTilesWithConstructableInstance) {
			if (constructableInstances.get(floor).get(checkTileId).getDto().getResultingSceneryId() == constructableId)
				return true;
		}
		
		return false;
	}
	
	public static int getRemainingTicks(int floor, int tileId) {
		if (!constructableInstances.containsKey(floor))
			return -1;
		
		if (!constructableInstances.get(floor).containsKey(tileId))
			return -1;
		
		return constructableInstances.get(floor).get(tileId).getRemainingTicks();
	}
	
	public static Constructable getConstructableInstanceByTileId(int floor, int tileId) {
		if (!constructableInstances.containsKey(floor))
			return null;
		
		if (!constructableInstances.get(floor).containsKey(tileId))
			return null;
		
		return constructableInstances.get(floor).get(tileId);
	}
}
