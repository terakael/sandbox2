package processing.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import processing.PathFinder;
import processing.attackable.Ship;
import responses.ResponseMaps;
import responses.ShipUiUpdateResponse;
import utils.Utils;

public class OceanFishingManager {
	private static Map<Integer, Map<Integer, Integer>> fishedTiles = new HashMap<>(); // floor, <tileId, counter>
	private static Map<Integer, Map<Integer, Integer>> babyColossals = new HashMap<>();
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
				
				tileMap.keySet().stream()
					.map(tileId -> LocationManager.getLocalShips(floor, tileId, MAX_RADIUS))
					.flatMap(Set::stream)
					.filter(Ship::canFish)
					.forEach(ship -> {
						ShipUiUpdateResponse.builder()
							.fishPopulation(ship.getFishPopulationString())
							.build()
							.passengerResponse(ship, responseMaps);
					});
				
				tileMap.keySet().removeAll(tileIdsToRemove);
			});
			
			babyColossals.forEach((floor, tileMap) -> {
				tileMap.replaceAll((tileId, counter) -> --counter);
				Set<Integer> tileIdsToRemove = tileMap.entrySet().stream()
					.filter(e -> e.getValue() <= 0)
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
				
				tileMap.keySet().stream()
					.map(tileId -> LocationManager.getLocalShips(floor, tileId, MAX_RADIUS))
					.flatMap(Set::stream)
					.filter(Ship::canFish)
					.forEach(ship -> {
						ShipUiUpdateResponse.builder()
							.fishPopulation(ship.getFishPopulationString())
							.build()
							.passengerResponse(ship, responseMaps);
					});
				
				tileMap.keySet().removeAll(tileIdsToRemove);
			});
		}
	}
	
	public static void increaseTileDifficulty(int floor, int tileId, ResponseMaps responseMaps) {
		fishedTiles.putIfAbsent(floor, new HashMap<>());
		fishedTiles.get(floor).merge(tileId, 1, Integer::sum);
		LocationManager.addOceanFishedTile(floor, tileId);
		
		LocationManager.getLocalShips(floor, tileId, MAX_RADIUS).stream()
			.filter(Ship::canFish)
			.forEach(ship -> {
				ShipUiUpdateResponse.builder()
					.fishPopulation(ship.getFishPopulationString())
					.build()
					.passengerResponse(ship, responseMaps);
			});
	}
	
	public static double getTileDifficulty(int floor, int tileId) {
		// 0 means full strength (i.e. nobody has fished near it)
		// 1 means all fished out (this tile has been fished the fuck out of)
		
		if (!fishedTiles.containsKey(floor))
			return 0;
		
		final Set<Integer> nearTileIds = LocationManager.getLocalFishedTiles(floor, tileId, MAX_RADIUS);
		return fishedTiles.get(floor).entrySet().stream()
			.filter(e -> nearTileIds.contains(e.getKey()))
			.map(e -> Math.max(0, (double)(Math.min(e.getValue(), MAX_RADIUS) - PathFinder.calculateDistance(e.getKey(), tileId)) / (double)MAX_RADIUS))
			.max(Double::compareTo)
			.orElse(0.0);
	}
	
	public static void addBabyColossalTile(int floor, int tileId) {
		babyColossals.putIfAbsent(floor, new HashMap<>());
		babyColossals.get(floor).put(tileId, 5);
	}
	
	public static void removeBabyColossalTile(int floor, int tileId) {
		if (!babyColossals.containsKey(floor))
			return;
		
		babyColossals.get(floor).keySet()
			.removeIf(t -> Utils.areTileIdsWithinRadius(tileId, t, 5));
		
		if (babyColossals.get(floor).isEmpty())
			babyColossals.remove(floor);
	}
	
	public static boolean isNearBabyColossalTile(int floor, int tileId) {
		if (!babyColossals.containsKey(floor))
			return false;
		
		return babyColossals.get(floor).keySet().stream()
			.anyMatch(t -> Utils.areTileIdsWithinRadius(tileId, t, 5));
	}
}
