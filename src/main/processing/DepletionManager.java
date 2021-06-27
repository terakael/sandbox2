package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import main.responses.ResponseMaps;
import main.responses.SceneryDepleteResponse;
import main.responses.SceneryRespawnResponse;

public class DepletionManager {
	public enum DepletionType {
		rock, flower, tree, chest
	}
	
	private static Map<Integer, Map<DepletionType, Map<Integer, Integer>>> depletedScenery = new HashMap<>(); // floor, type, <tileId, remainingTicks>>
	
	public static void process(ResponseMaps responseMaps) {
		depletedScenery.forEach((floor, type) -> {
			Set<Integer> allRespawnScenery = new HashSet<>(); 
			for (Map.Entry<DepletionType, Map<Integer, Integer>> entry : type.entrySet()) {
				// decrement by one tick
				entry.getValue().replaceAll((k, v) -> v -= 1);
				
				// pull all the tileIds which have just hit 0
				Set<Integer> respawnScenery = entry.getValue().entrySet().stream()
					.filter(e -> e.getValue() == 0)
					.map(e -> e.getKey())
					.collect(Collectors.toSet());
					
				// remove all the respawn elements from the map
				entry.getValue().keySet().removeIf(key -> respawnScenery.contains(key));
				
				allRespawnScenery.addAll(respawnScenery);
			}
			
			for (Integer tileId : allRespawnScenery) {
				SceneryRespawnResponse respawnResponse = new SceneryRespawnResponse();
				respawnResponse.setTileId(tileId);
				responseMaps.addLocalResponse(floor, tileId, respawnResponse);
			}
		});
	}
	
	public static boolean isDepleted(DepletionType type, int floor, int tileId) {
		if (!depletedScenery.containsKey(floor))
			return false;
		
		if (!depletedScenery.get(floor).containsKey(type))
			return false;
		
		return depletedScenery.get(floor).get(type).containsKey(tileId);
	}
	
	public static void addDepletedScenery(DepletionType type, int floor, int tileId, int ticks, ResponseMaps responseMaps) {
		if (!depletedScenery.containsKey(floor))
			depletedScenery.put(floor, new HashMap<>());
		
		if (!depletedScenery.get(floor).containsKey(type))
			depletedScenery.get(floor).put(type, new HashMap<>());
		
		depletedScenery.get(floor).get(type).put(tileId, ticks);
		
		SceneryDepleteResponse depleteResponse = new SceneryDepleteResponse();
		depleteResponse.setTileId(tileId);
		responseMaps.addLocalResponse(floor, tileId, depleteResponse);
	}
	
	public static Set<Integer> getDepletedSceneryTileIds(int floor) {
		Set<Integer> depletedIds = new HashSet<>();
		
		if (!depletedScenery.containsKey(floor))
			return depletedIds;
		
		
		depletedScenery.get(floor).forEach((k, v) -> {
			depletedIds.addAll(v.keySet());
		});
		
		return depletedIds;
	}
}
