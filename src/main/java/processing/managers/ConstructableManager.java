package processing.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.DbConnection;
import database.dao.ConstructableDao;
import database.dao.SceneryDao;
import database.dto.ConstructableDto;
import database.entity.delete.DeleteHousingConstructableEntity;
import database.entity.insert.InsertHousingConstructableEntity;
import processing.PathFinder;
import processing.scenery.constructable.BleedingTotemPole;
import processing.scenery.constructable.Constructable;
import processing.scenery.constructable.CrudeHull;
import processing.scenery.constructable.HolyTotemPole;
import processing.scenery.constructable.LargeStorageChest;
import processing.scenery.constructable.NaturesShrine;
import processing.scenery.constructable.SmallStorageChest;
import responses.ResponseMaps;
import responses.SceneryDespawnResponse;
import types.ConstructionLandTypes;
import utils.Utils;

public class ConstructableManager {
	private static Map<Integer, Class<? extends Constructable>> constructables = new HashMap<>(); // sceneryId, constructable class
	private static Map<Integer, Map<Integer, Constructable>> constructableInstances = new HashMap<>(); // floor, <tileId, constructable>
	private static Map<Integer, Set<Integer>> housingConstructableInstances = new HashMap<>(); // floor, <tile_ids>
	
	static {
		constructables.put(138, BleedingTotemPole.class);
		constructables.put(139, HolyTotemPole.class);
		constructables.put(140, NaturesShrine.class);
		constructables.put(141, SmallStorageChest.class);
		constructables.put(147, LargeStorageChest.class);
		constructables.put(184, CrudeHull.class);
	}
	
	public static void setupCaches() {
		housingConstructableInstances = new HashMap<>();
		
		DbConnection.load("select floor, tile_id, constructable_id from housing_constructables", rs -> {
			final int floor = rs.getInt("floor");
			final int tileId = rs.getInt("tile_id");
			final int constructableId = rs.getInt("constructable_id");
			
			housingConstructableInstances.putIfAbsent(floor, new HashSet<>());			
			housingConstructableInstances.get(floor).add(tileId);
			
			final Constructable constructableInstance = newConstructableInstance(
					HousingManager.getOwningPlayerId(floor, tileId),
					floor, 
					tileId, 
					ConstructableDao.getConstructableBySceneryId(constructableId), 
					Integer.MAX_VALUE,
					true,
					null);
			
			if (constructableInstance != null) {
				constructableInstances.putIfAbsent(floor, new HashMap<>());
				constructableInstances.get(floor).put(tileId, constructableInstance);
			}
		});
	}
	
	public static void process(int tickId, ResponseMaps responseMaps) {
		constructableInstances.forEach((floor, tileIdMap) -> {
			Set<Integer> tileIdsToRemove = new HashSet<>();
			tileIdMap.forEach((tileId, constructable) -> {
				constructable.process(tickId, responseMaps);

				if (constructable.getRemainingTicks() <= 0) {
					tileIdsToRemove.add(tileId);
					responseMaps.addLocalResponse(floor, tileId, new SceneryDespawnResponse(constructable.getDto().getResultingSceneryId(), tileId));
				}
			});
			
			constructableInstances.get(floor).keySet().removeIf(e -> tileIdsToRemove.contains(e));
		});
	}
	
	public static int getConstructableIdByTileId(int floor, int tileId) {
		if (!constructableInstances.containsKey(floor))
			return -1;
		
		if (!constructableInstances.get(floor).containsKey(tileId))
			return -1;
		
		return constructableInstances.get(floor).get(tileId).getDto().getResultingSceneryId();
	}
	
	public static void add(int playerId, int floor, int tileId, ConstructableDto constructable, int lifetimeTicks, ResponseMaps responseMaps) {
		if (!(((constructable.getLandType() & ConstructionLandTypes.land.getValue()) != 0 && PathFinder.tileIsWalkable(floor, tileId)) ||
			((constructable.getLandType() & ConstructionLandTypes.water.getValue()) != 0 && PathFinder.tileIsSailable(floor, tileId))))
			return;
		
		// if something already exists here then bail
		if (SceneryDao.getSceneryIdByTileId(floor, tileId) != -1 || getConstructableIdByTileId(floor, tileId) != -1)
			return;
		
		final boolean onHousingTile = HousingManager.getHouseIdFromFloorAndTileId(floor, tileId) > 0;
		
		final Constructable constructableInstance = newConstructableInstance(playerId, floor, tileId, constructable, lifetimeTicks, onHousingTile, responseMaps);
		if (constructableInstance == null)
			return;
		
		constructableInstances.putIfAbsent(floor, new HashMap<>());
		constructableInstances.get(floor).put(tileId, constructableInstance);
	
		// if it is built on player's housing land, then add the entry to the housing_constructables table.
		// does it matter whether the player that owns the house is the one to build it?
		// probably not; any constructable built on housing land should be permanent.
		if (onHousingTile) {
			housingConstructableInstances.putIfAbsent(floor, new HashSet<>());			
			housingConstructableInstances.get(floor).add(tileId);
			
			DatabaseUpdater.enqueue(new InsertHousingConstructableEntity(floor, tileId, constructable.getResultingSceneryId()));
		}
	}
	
	private static Constructable newConstructableInstance(int playerId, int floor, int tileId, ConstructableDto constructable, int lifetimeTicks, boolean onHousingTile, ResponseMaps responseMaps) {
		Constructable newConstructableInstance = null;
		if (constructables.containsKey(constructable.getResultingSceneryId())) {
			try {
				newConstructableInstance = constructables.get(constructable.getResultingSceneryId())
						.getDeclaredConstructor(int.class, int.class, int.class, int.class, ConstructableDto.class, boolean.class, ResponseMaps.class)
						.newInstance(playerId, floor, tileId, lifetimeTicks, constructable, onHousingTile, responseMaps);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else {
			newConstructableInstance = new Constructable(playerId, floor, tileId, lifetimeTicks, constructable, onHousingTile, responseMaps); // generic constructable, represents constructables that don't have special process logic
		}
		
		return newConstructableInstance;
	}
	
	public static boolean constructableIsInRadius(int floor, int tileId, int constructableId, int radius) {
		if (!constructableInstances.containsKey(floor))
			return false;
		
		final Set<Integer> allLocalTiles = Utils.getLocalTiles(tileId, radius);
		final Set<Integer> localTilesWithConstructableInstance = constructableInstances.get(floor).keySet().stream()
				.filter(e -> allLocalTiles.contains(e))
				.collect(Collectors.toSet());
		
		for (int checkTileId : localTilesWithConstructableInstance) {
			if (constructableInstances.get(floor).get(checkTileId).getDto().getResultingSceneryId() == constructableId)
				return true;
		}
		
		return false;
	}
	
	public static int getRemainingTicks(int floor, int tileId) {
		if (!constructableInstances.containsKey(floor))
			return -1;
		
		if (!constructableInstances.get(floor).containsKey(tileId))
			return -1;
		
		return constructableInstances.get(floor).get(tileId).getRemainingTicks();
	}
	
	public static Constructable getConstructableInstanceByTileId(int floor, int tileId) {
		if (!constructableInstances.containsKey(floor))
			return null;
		
		if (!constructableInstances.get(floor).containsKey(tileId))
			return null;
		
		return constructableInstances.get(floor).get(tileId);
	}
	
	public static void destroyConstructableInstanceByTileId(int floor, int tileId, ResponseMaps responseMaps) {
		Constructable instance = getConstructableInstanceByTileId(floor, tileId);
		if (instance == null)
			return;
		
		responseMaps.addLocalResponse(floor, tileId, new SceneryDespawnResponse(instance.getDto().getResultingSceneryId(), tileId));
		constructableInstances.get(floor).remove(tileId);
		
		if (housingConstructableInstances.get(floor).contains(tileId)) {
			housingConstructableInstances.get(floor).remove(tileId);
			DatabaseUpdater.enqueue(DeleteHousingConstructableEntity.builder().floor(floor).tileId(tileId).build());
		}
		
		instance.onDestroy(responseMaps);
	}
}
