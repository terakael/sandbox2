package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import main.database.dao.PickableDao;
import main.database.dto.PickableDto;
import main.responses.ResponseMaps;
import main.responses.SceneryDepleteResponse;
import main.responses.SceneryRespawnResponse;
import main.types.SceneryAttributes;

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
	
	public static void removeDaylightFlowers(boolean daylight) {		
		depletedScenery.forEach((floor, type) -> {
			if (floor >= 0) {
				for (Map.Entry<DepletionType, Map<Integer, Integer>> entry : type.entrySet()) {
					if (entry.getKey() == DepletionType.flower) {
						entry.getValue().keySet().removeIf(tileId -> {
							PickableDto pickable = PickableDao.getPickableByTileId(floor, tileId);
							return (daylight && !pickable.isDiurnal()) || (!daylight && !pickable.isNocturnal());
						});
					}
				}
			}
		});
	}
	
	public static boolean isDepleted(DepletionType type, int floor, int tileId) {
		if (type == DepletionType.flower) {
			// some flowers only come out during the day or night.
			PickableDto dto = PickableDao.getPickableByTileId(floor, tileId);
			if (dto == null || (WorldProcessor.isDaytime() && !dto.isDiurnal()) || (!WorldProcessor.isDaytime() && !dto.isNocturnal()))
				return true;
		}
		
		return isDepletedIgnoringDaytime(type, floor, tileId);
	}
	
	public static boolean isDepletedIgnoringDaytime(DepletionType type, int floor, int tileId) {
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
		
		PickableDao.getPickablesByFloor(floor).forEach((sceneryId, tileIds) -> {
			PickableDto dto = PickableDao.getPickableBySceneryId(sceneryId);
			if (dto != null && ((WorldProcessor.isDaytime() && !dto.isDiurnal()) || (!WorldProcessor.isDaytime() && !dto.isNocturnal())))
				depletedIds.addAll(tileIds);
		});
		
		if (!depletedScenery.containsKey(floor))
			return depletedIds;
		
		depletedScenery.get(floor).forEach((k, v) -> {
			depletedIds.addAll(v.keySet());
		});
		
		return depletedIds;
	}
}
