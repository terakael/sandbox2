package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

import main.responses.ResponseMaps;
import main.responses.RockRespawnResponse;

public class RockManager {
	private static HashMap<Integer, Integer> deadRocks = new HashMap<>();// tileId, remainingTicks
	public static void process(ResponseMaps responseMaps) {
		// decrement by one tick
		deadRocks.replaceAll((k, v) -> v -= 1);
		
		// pull all the tileIds which have just hit 0
		HashSet<Integer> respawnRocks = deadRocks.entrySet().stream()
			.filter(e -> e.getValue() == 0)
			.map(e -> e.getKey())
			.collect(Collectors.toCollection(HashSet::new));
			
		// remove all the respawn elements from the map
		deadRocks.keySet().removeIf(key -> respawnRocks.contains(key));
		
		for (Integer tileId : respawnRocks) {
			RockRespawnResponse rockRespawnResponse = new RockRespawnResponse();
			rockRespawnResponse.setTileId(tileId);
			responseMaps.addBroadcastResponse(rockRespawnResponse);
		}
	}
	
	public static boolean rockIsDepleted(int tileId) {
		return deadRocks.containsKey(tileId);
	}
	
	public static void addDepletedRock(int tileId, int ticks) {
		deadRocks.put(tileId, ticks);
	}
	
	public static HashSet<Integer> getDepletedRockTileIds() {
		return new HashSet<>(deadRocks.keySet());
	}
}
