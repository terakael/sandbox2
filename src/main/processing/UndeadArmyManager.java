package main.processing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import main.database.dao.NPCDao;
import main.database.dao.UndeadArmyWavesDao;
import main.database.dto.NPCDto;
import main.responses.AddSceneryInstancesResponse;
import main.responses.NpcOutOfRangeResponse;
import main.responses.ResponseMaps;
import main.responses.SceneryDespawnResponse;

public class UndeadArmyManager {
	private static boolean alreadyInitialized = false;
	private static int currentWave = 0;
	private static Map<Integer, UndeadArmyNpc> currentWaveNpcs = new HashMap<>();
	
	private static int entWave = 2;
	private static NPCDto entDto = NPCDao.getNpcById(48); // ent TODO dead ent
	private static final int entSceneryId = 9;// dead tree
	
	// before wave 50 and during the day these are just the locations of dead trees.
	// on wave 50 during the night they become ent npcs' instanceIds
	private static Set<Integer> undeadEntLocations = Set.<Integer>of(937221352,
																	936989734,
																	937591951,
																	937360337,
																	937499322,
																	937777267,
																	937730947,
																	937221364);
	
	public static void init(ResponseMaps responseMaps) {
		if (alreadyInitialized)
			return;
		alreadyInitialized = true;
		resetEnts(responseMaps);
	}
	
	public static void reset(ResponseMaps responseMaps) {		
		currentWave = 0;
		newWave(responseMaps);
	}
	
	public static void onDaytimeChange(boolean isDaytime, ResponseMaps responseMaps) {
		if (isDaytime) {
			currentWave = 0;
			resetEnts(responseMaps);
			clearExistingNpcs(responseMaps);
		} else {
			reset(responseMaps);
		}
	}
	
	public static void checkWaveStatus(ResponseMaps responseMaps) {
		if (currentWaveNpcs.isEmpty())
			return; // if we exceeded the max waves then this will be empty
		
		if (currentWaveNpcs.values().stream().filter(e -> e.getCurrentHp() > 0).findAny().isEmpty())
			newWave(responseMaps);
	}
	
	private static void newWave(ResponseMaps responseMaps) {
		clearExistingNpcs(responseMaps);
		++currentWave;
		
		UndeadArmyWavesDao.getWave(currentWave).forEach(dto -> {
			NPCDto npcDto = NPCDao.getNpcById(dto.getNpcId());
			if (npcDto != null) {
				// annoying hack because the NpcDto class is written badly.
				// TODO modernize the dto class so it doesn't have instance data.
				NPCDto deepCopy = new NPCDto(npcDto);
				deepCopy.setFloor(0);// undead army is on the ground floor
				deepCopy.setTileId(dto.getTileId());
				
				currentWaveNpcs.put(dto.getTileId(), new UndeadArmyNpc(deepCopy));
			}
		});
		
		if (currentWave == entWave) {
			// the locations of the trees are no longer non-walkable
			undeadEntLocations.forEach(tileId -> {
				PathFinder.setImpassabilityOnTileId(0, tileId, 0); // when the trees aren't scenery, players can walk on the tile.
				responseMaps.addLocalResponse(0, tileId, new SceneryDespawnResponse(tileId));
				
				NPCDto deepCopy = new NPCDto(entDto);
				deepCopy.setFloor(0);// undead army is on the ground floor
				deepCopy.setTileId(tileId);
				currentWaveNpcs.put(tileId, new UndeadArmyNpc(deepCopy));
			});
		}
		
		// if we exceed the max waves then this will be empty.
		if (!currentWaveNpcs.isEmpty())
			LocationManager.addNpcs(currentWaveNpcs.values().stream().collect(Collectors.toList()));
	}
	
	private static void clearExistingNpcs(ResponseMaps responseMaps) {
		currentWaveNpcs.forEach((instanceId, npc) -> {
			LocationManager.removeNpc(npc);
			NpcOutOfRangeResponse outOfRangeResponse = new NpcOutOfRangeResponse();
			outOfRangeResponse.setInstances(Collections.singleton(instanceId));
			responseMaps.addLocalResponse(0, npc.getTileId(), outOfRangeResponse);
		});
		currentWaveNpcs.clear();
	}
	
	private static void resetEnts(ResponseMaps responseMaps) {
		undeadEntLocations.forEach(tileId -> {
			PathFinder.setImpassabilityOnTileId(0, tileId, 15); // completely impassable
			
			// there might be some players that have not loaded a dead tree that arrived as the trees were ents
			LocationManager.getLocalPlayers(0, tileId, 12).forEach(player -> {
				ClientResourceManager.addLocalScenery(player, Collections.singleton(9));
				
				AddSceneryInstancesResponse inRangeResponse = new AddSceneryInstancesResponse();
				inRangeResponse.setInstances(Map.<Integer, Set<Integer>>of(9, Collections.singleton(tileId)));
				
				// whenever we update the scenery the doors/depleted scenery are reset, so we need to reset them.
				inRangeResponse.setOpenDoors(player.getFloor(), player.getLocalTiles());
				inRangeResponse.setDepletedScenery(player.getFloor(), player.getLocalTiles());
				responseMaps.addClientOnlyResponse(player, inRangeResponse);
			});
		});
	}
	
	public static NPC getNpcByInstanceId(int instanceId) {
		return currentWaveNpcs.get(instanceId);
	}
	
	public static int getSceneryIdByTileId(int tileId) {
		if (undeadEntLocations.contains(tileId) && currentWave < entWave) {
			return entSceneryId; // dead tree
		}
		return -1;
	}
}
