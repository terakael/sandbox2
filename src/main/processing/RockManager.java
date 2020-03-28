package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import main.responses.ResponseMaps;
import main.responses.RockRespawnResponse;

public class RockManager {
	private static Map<Integer, Map<Integer, Integer>> deadRocks = new HashMap<>();// floor, <tileId, remainingTicks>
	public static void process(ResponseMaps responseMaps) {
		for (Map.Entry<Integer, Map<Integer, Integer>> entry : deadRocks.entrySet()) {
			// decrement by one tick
			entry.getValue().replaceAll((k, v) -> v -= 1);
			
			// pull all the tileIds which have just hit 0
			Set<Integer> respawnRocks = entry.getValue().entrySet().stream()
				.filter(e -> e.getValue() == 0)
				.map(e -> e.getKey())
				.collect(Collectors.toSet());
				
			// remove all the respawn elements from the map
			entry.getValue().keySet().removeIf(key -> respawnRocks.contains(key));
			
			for (Integer tileId : respawnRocks) {
				RockRespawnResponse rockRespawnResponse = new RockRespawnResponse();
				rockRespawnResponse.setTileId(tileId);
				responseMaps.addLocalResponse(entry.getKey(), tileId, rockRespawnResponse);
			}
		}
	}
	
	public static boolean rockIsDepleted(int floor, int tileId) {
		return deadRocks.containsKey(floor) && deadRocks.get(floor).containsKey(tileId);
	}
	
	public static void addDepletedRock(int floor, int tileId, int ticks) {
		if (!deadRocks.containsKey(floor))
			deadRocks.put(floor, new HashMap<>());
		deadRocks.get(floor).put(tileId, ticks);
	}
	
	public static HashSet<Integer> getDepletedRockTileIds(int floor) {
		if (!deadRocks.containsKey(floor))
			return new HashSet<>();
		return new HashSet<>(deadRocks.get(floor).keySet());
	}
}
