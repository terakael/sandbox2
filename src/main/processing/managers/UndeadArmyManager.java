package main.processing.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import main.database.dao.NPCDao;
import main.database.dao.SceneryDao;
import main.database.dao.UndeadArmyWavesDao;
import main.database.dto.NPCDto;
import main.processing.PathFinder;
import main.processing.attackable.NPC;
import main.processing.attackable.NecromancerFirstForm;
import main.processing.attackable.NecromancerSecondForm;
import main.processing.attackable.Player;
import main.processing.attackable.PlayerGrownZombie;
import main.processing.attackable.UndeadArmyNpc;
import main.responses.AddSceneryInstancesResponse;
import main.responses.MessageResponse;
import main.responses.NpcInRangeResponse;
import main.responses.NpcOutOfRangeResponse;
import main.responses.PvmStartResponse;
import main.responses.ResponseMaps;
import main.responses.SceneryDespawnResponse;
import main.responses.TalkToResponse;
import main.responses.TeleportExplosionResponse;
import main.types.NpcAttributes;
import main.utils.RandomUtil;
import main.utils.Utils;

public class UndeadArmyManager {
	private static boolean alreadyInitialized = false;
	private static int currentWave = 0;
	@Getter private static Map<Integer, UndeadArmyNpc> currentWaveNpcs = new HashMap<>();
	private static Map<Integer, Map<Integer, PlayerGrownZombie>> playerGrownZombies = new HashMap<>();
	
	private static int entWave = 32;
	private static NPCDto entDto = NPCDao.getNpcById(57); // ent
	private static final int entSceneryId = 9;// dead tree
	
	private static final int firstFormNecromancerNpcId = 55;
	private static final int secondFormNecromancerNpcId = 56;
	private static final int graveyardCentreTileId = 937406662;
	
	private static int timer = 0;
	private static boolean initWaveAfterTimer = false;
	
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
	
	public static void setWave(int wave, ResponseMaps responseMaps) {
		currentWave = wave - 1;
		newWave(responseMaps);
	}
	
	public static void reset(ResponseMaps responseMaps) {		
		currentWave = 0;
		newWave(responseMaps);
	}
	
	public static void process(ResponseMaps responseMaps) {
		if (!alreadyInitialized) {
			alreadyInitialized = true;
			resetEnts(responseMaps);
		}
		
		if (timer > 0) {
			if (--timer == 0) {
				if (initWaveAfterTimer) {
					initWaveAfterTimer = false;
					newWave(responseMaps);
				}
			}
		}
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
		
		if (currentWaveNpcs.values().stream().filter(e -> e.getCurrentHp() > 0).findAny().isEmpty()) {
			if (currentWave == entWave) {
				// the next wave is the second form necromancer, so add a delay
				newWaveAfterTimer(10, responseMaps); // nice six second delay
				responseMaps.addLocalResponse(0, graveyardCentreTileId, MessageResponse.newMessageResponse("necromancer: you killed all my trees...", "yellow"));
			} else 
				newWave(responseMaps);
		}
	}
	
	public static void newWaveAfterTimer(int ticks, ResponseMaps responseMaps) {
		clearExistingNpcs(responseMaps);
		timer = ticks;
		initWaveAfterTimer = true;
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
		
		if (currentWave == entWave - 1) {
			// this is the wave the first-form necromancer spawns.
			NPCDto deepCopy = new NPCDto(NPCDao.getNpcById(firstFormNecromancerNpcId));
			deepCopy.setFloor(0);
			deepCopy.setTileId(graveyardCentreTileId);
			currentWaveNpcs.put(graveyardCentreTileId, new NecromancerFirstForm(deepCopy));
		} else if (currentWave == entWave) {
			// the locations of the trees are no longer non-walkable
			undeadEntLocations.forEach(tileId -> {
				PathFinder.setImpassabilityOnTileId(0, tileId, 0); // when the trees aren't scenery, players can walk on the tile.
				responseMaps.addLocalResponse(0, tileId, new SceneryDespawnResponse(tileId));
				
				NPCDto deepCopy = new NPCDto(entDto);
				deepCopy.setFloor(0);// undead army is on the ground floor
				deepCopy.setTileId(tileId);
				currentWaveNpcs.put(tileId, new UndeadArmyNpc(deepCopy));
			});
		} else if (currentWave == entWave + 1) {
			// second form necromancer.
			final List<Player> localPlayers = getPlayersInGraveyard().stream()
					.filter(player -> !FightManager.fightWithFighterExists(player)) // exclude any players currently fighting (could be duelling or whatevs)
					.collect(Collectors.toList());
			
			// if we choose a player inside the crypt room, then the necromancer will teleport inside, then nobody will be able to attack him if they don't have the key
			final List<Player> playersOutsideCryptRoom = localPlayers.stream()
					.filter(player -> !Utils.tileIdWithinRect(player.getTileId(), 937591957, 937684609))
					.collect(Collectors.toList());
			
			Player targetPlayer = null;
			if (!playersOutsideCryptRoom.isEmpty()) {
				targetPlayer = playersOutsideCryptRoom.get(RandomUtil.getRandom(0, playersOutsideCryptRoom.size()));
			}
			
			final int tileId = targetPlayer == null ? graveyardCentreTileId : targetPlayer.getTileId(); // idk here i guess
			NPCDto deepCopy = new NPCDto(NPCDao.getNpcById(secondFormNecromancerNpcId));
			deepCopy.setFloor(0);
			deepCopy.setTileId(graveyardCentreTileId); // for the instanceId
			NecromancerSecondForm necromancer = new NecromancerSecondForm(deepCopy);
			
			
			currentWaveNpcs.put(graveyardCentreTileId, necromancer);
			
			if (targetPlayer != null) {
				FightManager.addFight(targetPlayer, necromancer, false);
				
				// we're forced to hack the npcInRangeResponse here because 
				// the client needs to recognize the npc before the pvm start message appears.
				necromancer.setTileId(tileId); // for the actual location of the npc
				NpcInRangeResponse npcInRangeResponse = new NpcInRangeResponse();
				npcInRangeResponse.addInstances(0, Collections.singleton(necromancer.getInstanceId()));
				responseMaps.addLocalResponse(0, tileId, npcInRangeResponse);
				
				PvmStartResponse pvmStart = new PvmStartResponse();
				pvmStart.setPlayerId(targetPlayer.getId());
				pvmStart.setMonsterId(necromancer.getInstanceId());
				pvmStart.setTileId(tileId);
				responseMaps.addLocalResponse(0, tileId, pvmStart);
				
				localPlayers.forEach(player -> {
					player.getInRangeNpcs().add(necromancer.getInstanceId());
					ClientResourceManager.addNpcs(player, Collections.singleton(necromancer.getId()));
				});
			}
			
			final String message = "you will pay for this!";
			responseMaps.addLocalResponse(0, tileId, new TeleportExplosionResponse(tileId));
			responseMaps.addLocalResponse(0, tileId, MessageResponse.newMessageResponse(String.format("necromancer: %s", message), "yellow"));
			responseMaps.addLocalResponse(0, tileId, new TalkToResponse(necromancer.getInstanceId(), message));
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
			
			if (SceneryDao.getSceneryIdByTileId(0, tileId) == -1) {
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
			}
		});
	}
	
	public static NPC getNpcByInstanceId(int floor, int instanceId) {
		if (currentWaveNpcs.containsKey(instanceId))
			return currentWaveNpcs.get(instanceId);
		
		if (!playerGrownZombies.containsKey(floor))
			return null;
		return playerGrownZombies.get(floor).get(instanceId);
	}
	
	public static int getSceneryIdByTileId(int floor, int tileId) {
		if (floor != 0)
			return -1;
		
		if (undeadEntLocations.contains(tileId) && currentWave < entWave) {
			return entSceneryId; // dead tree
		}
		return -1;
	}
	
	public static int getNumAliveNpcsInCurrentWave() {
		return getAliveNpcsInCurrentWave().size();
	}
	
	public static Set<UndeadArmyNpc> getAliveNpcsInCurrentWave() {
		return currentWaveNpcs.values().stream().filter(e -> e.getCurrentHp() > 0).collect(Collectors.toSet());
	}
	
	public static Set<Player> getPlayersInGraveyard() {
		return LocationManager.getLocalPlayersWithinRect(0, 936943400, 937823599);
	}
	
	public static List<Player> getPlayersInGraveyardExcludingCrypt() {
		return getPlayersInGraveyard().stream()
				.filter(player -> !Utils.tileIdWithinRect(player.getTileId(), 937591957, 937684609))
				.collect(Collectors.toList());
	}
	
	public static void addPlayerGrownZombie(Player planter, int floor, int tileId) {
		int npcId = 51; // reg zombie by default
		if (RandomUtil.chance(1)) {
			npcId = 57;
		} else if (RandomUtil.chance(10)) {
			npcId = 53;
		} else if (RandomUtil.chance(15)) {
			npcId = 52;
		} else if (RandomUtil.chance(25)) {
			npcId = 39;
		}
		
		
		NPCDto deepCopy = new NPCDto(NPCDao.getNpcById(npcId));
		deepCopy.setFloor(floor);
		deepCopy.setTileId(tileId); // for the instanceId
		deepCopy.setAttributes(deepCopy.getAttributes() & ~NpcAttributes.AGGRESSIVE.getValue()); // they should be unaggressive otherwise mad griefing ensues
		deepCopy.setAttributes(deepCopy.getAttributes() | NpcAttributes.DIURNAL.getValue()); // should show at all times of the day
		deepCopy.setAttributes(deepCopy.getAttributes() | NpcAttributes.NOCTURNAL.getValue());
		deepCopy.setRespawnTicks(5); // onRespawn is where the zombie is removed from the game (onDeath is too early as we wanna see the death animation)
		PlayerGrownZombie zombie = new PlayerGrownZombie(deepCopy);
		zombie.setPlanter(planter);
		playerGrownZombies.putIfAbsent(floor, new HashMap<>());
		playerGrownZombies.get(floor).put(tileId, zombie);
		
		LocationManager.addNpcs(Collections.singletonList(zombie));
	}
	
	public static void removePlayerGrownZombie(PlayerGrownZombie zombie) {
		playerGrownZombies.get(zombie.getFloor()).remove(zombie.getInstanceId());
		LocationManager.removeNpc(zombie);
	}
}
