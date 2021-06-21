package main.processing;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import main.database.dto.ConstructableDto;
import main.responses.ConstructableDespawnResponse;
import main.responses.ResponseMaps;

public class ConstructableManager {
	private static Map<Integer, Map<Integer, Integer>> constructableInstances = new HashMap<>(); // floor, <tileId, constructableId>
	private static Map<Integer, Map<Integer, Integer>> constructableLifetime = new HashMap<>(); // floor, <tileId, lifetime>
	
	public static void process(ResponseMaps responseMaps) {
		constructableLifetime.forEach((floor, tileIdMap) -> {
			
			tileIdMap.replaceAll((k, v) -> v -= 1);
			
			Set<Integer> tileIdsToRemove = tileIdMap.entrySet().stream()
					.filter(e -> e.getValue() == 0)
					.map(e -> e.getKey())
					.collect(Collectors.toSet());
			
			tileIdsToRemove.forEach(tileId -> 
				responseMaps.addLocalResponse(floor, tileId, new ConstructableDespawnResponse(tileId)));
			
			
			constructableInstances.get(floor).keySet().removeIf(e -> tileIdsToRemove.contains(e));
			constructableLifetime.get(floor).keySet().removeIf(e -> tileIdsToRemove.contains(e));
		});
	}
	
	public static int getConstructableIdByTileId(int floor, int tileId) {
		if (!constructableInstances.containsKey(floor))
			return -1;
		
		if (!constructableInstances.get(floor).containsKey(tileId))
			return -1;
		
		return constructableInstances.get(floor).get(tileId);
	}
	
	public static void add(int floor, int tileId, ConstructableDto constructable) {
		constructableInstances.putIfAbsent(floor, new HashMap<>());
		constructableInstances.get(floor).put(tileId, constructable.getResultingSceneryId());
		
		constructableLifetime.putIfAbsent(floor, new HashMap<>());
		constructableLifetime.get(floor).put(tileId, constructable.getLifetimeTicks());
	}
	
	public static boolean constructableIsInRadius(int floor, int tileId, int constructableId, int radius) {
		if (!constructableInstances.containsKey(floor))
			return false;
		
		final Set<Integer> allLocalTiles = WorldProcessor.getLocalTiles(tileId, radius);
		final Set<Integer> localTilesWithConstructableInstance = constructableInstances.get(floor).keySet().stream()
				.filter(e -> allLocalTiles.contains(e))
				.collect(Collectors.toSet());
		
		for (int checkTileId : localTilesWithConstructableInstance) {
			if (constructableInstances.get(floor).get(checkTileId) == constructableId)
				return true;
		}
		
		return false;
	}
}
