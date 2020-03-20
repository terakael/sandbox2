package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

import main.responses.FlowerRespawnResponse;
import main.responses.ResponseMaps;

public class FlowerManager {
	private static HashMap<Integer, Integer> deadFlowers = new HashMap<>();// tileId, remainingTicks
	public static void process(ResponseMaps responseMaps) {
		// decrement by one tick
		deadFlowers.replaceAll((k, v) -> v -= 1);
		
		// pull all the tileIds which have just hit 0
		HashSet<Integer> respawnFlowers = deadFlowers.entrySet().stream()
			.filter(e -> e.getValue() == 0)
			.map(e -> e.getKey())
			.collect(Collectors.toCollection(HashSet::new));
			
		// remove all the respawn elements from the map
		deadFlowers.keySet().removeIf(key -> respawnFlowers.contains(key));
		
		for (Integer tileId : respawnFlowers) {
			FlowerRespawnResponse flowerRespawnResponse = new FlowerRespawnResponse();
			flowerRespawnResponse.setTileId(tileId);
			responseMaps.addBroadcastResponse(flowerRespawnResponse);
		}
	}
	
	public static boolean flowerIsDepleted(int tileId) {
		return deadFlowers.containsKey(tileId);
	}
	
	public static void addDepletedFlower(int tileId, int ticks) {
		deadFlowers.put(tileId, ticks);
	}
	
	public static HashSet<Integer> getDepletedFlowerTileIds() {
		return new HashSet<Integer>(deadFlowers.keySet());
	}
}
