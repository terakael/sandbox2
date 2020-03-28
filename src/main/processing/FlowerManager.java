package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import main.responses.FlowerRespawnResponse;
import main.responses.ResponseMaps;

public class FlowerManager {
	private static Map<Integer, Map<Integer, Integer>> deadFlowers = new HashMap<>();// floor, <tileId, remainingTicks>
	public static void process(ResponseMaps responseMaps) {
		for (Map.Entry<Integer, Map<Integer, Integer>> entry : deadFlowers.entrySet()) {
			// decrement by one tick
			entry.getValue().replaceAll((k, v) -> v -= 1);
			
			// pull all the tileIds which have just hit 0
			Set<Integer> respawnFlowers = entry.getValue().entrySet().stream()
				.filter(e -> e.getValue() == 0)
				.map(e -> e.getKey())
				.collect(Collectors.toSet());
				
			// remove all the respawn elements from the map
			entry.getValue().keySet().removeIf(key -> respawnFlowers.contains(key));
			
			for (Integer tileId : respawnFlowers) {
				FlowerRespawnResponse flowerRespawnResponse = new FlowerRespawnResponse();
				flowerRespawnResponse.setTileId(tileId);
				responseMaps.addLocalResponse(entry.getKey(), tileId, flowerRespawnResponse);
			}
		}
	}
	
	public static boolean flowerIsDepleted(int floor, int tileId) {
		return deadFlowers.containsKey(floor) && deadFlowers.get(floor).containsKey(tileId);
	}
	
	public static void addDepletedFlower(int floor, int tileId, int ticks) {
		if (!deadFlowers.containsKey(floor))
			deadFlowers.put(floor, new HashMap<>());
		deadFlowers.get(floor).put(tileId, ticks);
	}
	
	public static HashSet<Integer> getDepletedFlowerTileIds(int floor) {
		if (!deadFlowers.containsKey(floor))
			return new HashSet<>();
		return new HashSet<Integer>(deadFlowers.get(floor).keySet());
	}
}
