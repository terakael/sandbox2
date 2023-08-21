package processing.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import processing.PathFinder;
import responses.ResponseMaps;

public class OceanFishingManager {
	private static Map<Integer, Map<Integer, Integer>> fishedTiles = new HashMap<>(); // floor, <tileId, counter>
	private static final int MAX_RADIUS = 6;
	
	public static void process(long tick, ResponseMaps responseMaps) {
		if (tick % 100 == 0) {
			fishedTiles.forEach((floor, tileMap) -> {
				tileMap.replaceAll((tileId, counter) -> --counter);
				Set<Integer> tileIdsToRemove = tileMap.entrySet().stream()
					.filter(e -> e.getValue() <= 0)
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
				
				LocationManager.removeOceanFishedTilesIfExist(floor, tileIdsToRemove);
				tileMap.keySet().removeAll(tileIdsToRemove);
			});
		}
	}
	
	public static void increaseTileDifficulty(int floor, int tileId) {
		fishedTiles.putIfAbsent(floor, new HashMap<>());
		fishedTiles.get(floor).merge(tileId, 1, Integer::sum);
		LocationManager.addOceanFishedTile(floor, tileId);
	}
	
	public static double getTileDifficulty(int floor, int tileId) {
		// 0 means full strength (i.e. nobody has fished near it)
		// 1 means all fished out (this tile has been fished the fuck out of)
		
		if (!fishedTiles.containsKey(floor))
			return 0;
		
		final Set<Integer> nearTileIds = LocationManager.getLocalFishedTiles(floor, tileId, MAX_RADIUS);
		return fishedTiles.get(floor).entrySet().stream()
			.filter(e -> nearTileIds.contains(e.getKey()))
			.map(e -> (double)(Math.min(e.getValue(), MAX_RADIUS) - PathFinder.calculateDistance(e.getKey(), tileId)) / (double)MAX_RADIUS)
			.max(Double::compareTo)
			.orElse(0.0);
	}
}
