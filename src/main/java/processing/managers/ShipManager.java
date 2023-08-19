package processing.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.dto.ShipDto;
import processing.attackable.Player;
import processing.attackable.Ship;
import responses.ResponseMaps;

public class ShipManager {
	private static Map<Integer, Ship> shipsByPlayerId = new HashMap<>();
	private static Map<Integer, Map<Integer, Ship>> hulls = new HashMap<>(); // floor, tileId, hull
	
	public static void addHull(int floor, int tileId, int playerId, ShipDto dto, ResponseMaps responseMaps) {
		// if a ship or hull already exists with this player, clear it out
		final Ship existingHull = getHullByPlayerId(playerId);
		if (existingHull != null) {
			System.out.println("found existing hull, removing");
			removeHull(existingHull.getFloor(), existingHull.getTileId(), responseMaps);
		}
		
		// if there's an existing ship then destroy it
		destroyShip(playerId);
		
		
		hulls.putIfAbsent(floor, new HashMap<>());
		
		final Ship hull = new Ship(playerId, dto);
		hull.setFloor(floor);
		hull.setTileId(tileId);
		hulls.get(floor).put(tileId, hull);
	}
	
	public static Ship getHullByPlayerId(int playerId) {
		return hulls.values().stream()
			.flatMap(hullsByTileId -> hullsByTileId.values().stream())
			.filter(ship -> ship.getCaptainId() == playerId)
			.findFirst()
			.orElse(null);
	}
	
	public static Ship removeHull(int floor, int tileId, ResponseMaps responseMaps) {
		if (hulls.containsKey(floor)) {
			final Ship removedHull = hulls.get(floor).remove(tileId); 
			return removedHull;
		}
		return null;
	}
	
	public static void finishShip(int floor, int tileId, ResponseMaps responseMaps) {		
		final Ship ship = removeHull(floor, tileId, responseMaps);
		if (ship == null)
			return;
		
		// remove the hull scenery
		ConstructableManager.destroyConstructableInstanceByTileId(floor, tileId, responseMaps);
		
		// if a ship already exists from this player, get rid of it
		destroyShip(ship.getCaptainId());
		
		shipsByPlayerId.put(ship.getCaptainId(), ship);
		LocationManager.addShip(ship);
	}
	
	public static void destroyShip(int captainId) {
		final Ship destroyedShip = shipsByPlayerId.remove(captainId);
		if (destroyedShip == null)
			return;
		LocationManager.removeShipIfExists(destroyedShip);
	}
	
	public static int getShipCaptainId(int floor, int tileId) {
		for (var ship : shipsByPlayerId.values()) {
			if (ship.getFloor() == floor && ship.getTileId() == tileId)
				return ship.getCaptainId();
		}
		return -1;
	}
	
	public static Ship getShipByCaptainId(int captainId) {
		return shipsByPlayerId.get(captainId);
	}
	
	public static Set<Ship> getShipsAt(int floor, int tileId) {
		return shipsByPlayerId.values().stream()
				.filter(ship -> ship.getFloor() == floor && ship.getTileId() == tileId)
				.collect(Collectors.toSet());
	}
	
	public static Ship getShipWithPlayer(Player player) {
		for (Ship ship : LocationManager.getLocalShips(player.getFloor(), player.getTileId(), 3)) {
			if (ship.playerIsAboard(player.getId()))
				return ship;
		}
		return null;
	}
	
	public static void process(int tick, ResponseMaps responseMaps) {
		shipsByPlayerId.values().forEach(ship -> ship.process(tick, responseMaps));
	}
	
//	public static int getShipIdByAboardPlayerId(int playerId) {
//		// shipId is just the playerId (but the one aboard may not be the captain)
//		return shipsByPlayerId.values().stream()
//			.filter(ship -> ship.playerIsAboard(playerId))
//			.map(Ship::getCaptainId)
//			.findFirst()
//			.orElse(-1);
//	}
}
