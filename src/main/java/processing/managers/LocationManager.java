package processing.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import processing.PathFinder;
import processing.attackable.NPC;
import processing.attackable.Player;
import types.NpcAttributes;
import utils.Utils;

public class LocationManager {
	private static final int SEGMENT_SIZE = 25;
	private static final int SEGMENTS_PER_ROW = PathFinder.LENGTH / SEGMENT_SIZE;
	
	private static Map<Integer, Map<Integer, Set<NPC>>> nocturnalNpcs = new HashMap<>();
	private static Map<Integer, Map<Integer, Set<NPC>>> diurnalNpcs = new HashMap<>();
	private static Map<Integer, Map<Integer, Set<NPC>>> undergroundNpcs = new HashMap<>();
	private static Map<Integer, Map<Integer, Set<NPC>>> pets = new HashMap<>();
	private static Map<Integer, Map<Integer, Set<Player>>> players = new HashMap<>();
	
	public static NPC getNpcNearPlayerByInstanceId(Player player, int instanceId) {
		return LocationManager.getLocalNpcs(player.getFloor(), player.getTileId(), 12).stream()
				.filter(e -> e.getInstanceId() == instanceId)
				.findFirst()
				.orElse(null);
	}
	
	public static Set<NPC> getLocalNpcs(int floor, int tileId, int radius) {
		return getLocalNpcs(floor, tileId, radius, TimeManager.isDaytime());
	}
	
	public static Set<NPC> getLocalNpcs(int floor, int tileId, int radius, boolean isDaytime) {
		final Map<Integer, Map<Integer, Set<NPC>>> sourceMap = 
				floor < 0
					? undergroundNpcs
					: isDaytime
						? diurnalNpcs
						: nocturnalNpcs;
		
		final int playerTileIdX = tileId % PathFinder.LENGTH;
		final int playerTileIdY = tileId / PathFinder.LENGTH;
		
		Set<NPC> localNpcs = new HashSet<>();
		getLocalSegments(tileId, radius).forEach(segment -> {
			if (pets.containsKey(floor) && pets.get(floor).containsKey(segment)) {
				localNpcs.addAll(pets.get(floor).get(segment).stream()
					.filter(npc -> {
						final int npcTileIdX = npc.getTileId() % PathFinder.LENGTH;
						final int npcTileIdY = npc.getTileId() / PathFinder.LENGTH;
						
						return Math.abs(playerTileIdX - npcTileIdX) <= radius && 
							   Math.abs(playerTileIdY - npcTileIdY) <= radius;
					}).collect(Collectors.toSet()));
			}
			
			if (sourceMap.containsKey(floor) && sourceMap.get(floor).containsKey(segment)) {
				localNpcs.addAll(sourceMap.get(floor).get(segment).stream()
					.filter(npc -> {
						final int npcTileIdX = npc.getTileId() % PathFinder.LENGTH;
						final int npcTileIdY = npc.getTileId() / PathFinder.LENGTH;
						
						return Math.abs(playerTileIdX - npcTileIdX) <= radius && 
							   Math.abs(playerTileIdY - npcTileIdY) <= radius;
					}).collect(Collectors.toSet()));
			}
		});
		
		return localNpcs;
	}
	
	public static Map<Integer, Set<NPC>> getAllNpcsNearPlayers(boolean isDaytime) {
		Map<Integer, Set<NPC>> npcsToReturn = new HashMap<>();
		
		players.forEach((floor, segmentMap) -> {
			final Map<Integer, Map<Integer, Set<NPC>>> sourceMap = 
					floor < 0
						? undergroundNpcs
						: isDaytime
							? diurnalNpcs
							: nocturnalNpcs;
			
			if (sourceMap.containsKey(floor)) {
				segmentMap.keySet().forEach(segment -> {
					if (sourceMap.get(floor).containsKey(segment)) {
						npcsToReturn.putIfAbsent(floor, new HashSet<>());
						npcsToReturn.get(floor).addAll(sourceMap.get(floor).get(segment));
					}
				});
			}
		});
		
		// pets are either following the player or wandering around a house.
		// pets wandering around a house far away from players don't need processing.
		// TODO filter this for pets that are following players plus pets in houses near players
		// check getLocalNpcs
		pets.forEach((floor, petMap) -> {
			npcsToReturn.putIfAbsent(floor, new HashSet<>());
			npcsToReturn.get(floor).addAll(petMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
		});
		
		return npcsToReturn;
	}
	
	public static void addNpcs(List<NPC> allNpcs) {
		allNpcs.forEach(npc -> {
			// local segments are the one that the spawn tile lies in, plus sibling segments if the roam radius intersects.
			// this means if a spawn tile is on the corner of a segment, there will be four segments here as the 
			// roam range will intersect all four.
			getLocalSegments(npc.getInstanceId(), npc.getDto().getRoamRadius()).forEach(segment -> {
				if (npc.getFloor() < 0) {
					undergroundNpcs.putIfAbsent(npc.getFloor(), new HashMap<>());
					undergroundNpcs.get(npc.getFloor()).putIfAbsent(segment, new HashSet<>());
					undergroundNpcs.get(npc.getFloor()).get(segment).add(npc);
				} else {
					if ((npc.getDto().getAttributes() & NpcAttributes.NOCTURNAL.getValue()) > 0) {
						nocturnalNpcs.putIfAbsent(npc.getFloor(), new HashMap<>());
						nocturnalNpcs.get(npc.getFloor()).putIfAbsent(segment, new HashSet<>());
						nocturnalNpcs.get(npc.getFloor()).get(segment).add(npc);
					}
					
					if ((npc.getDto().getAttributes() & NpcAttributes.DIURNAL.getValue()) > 0) {
						diurnalNpcs.putIfAbsent(npc.getFloor(), new HashMap<>());
						diurnalNpcs.get(npc.getFloor()).putIfAbsent(segment, new HashSet<>());
						diurnalNpcs.get(npc.getFloor()).get(segment).add(npc);
					}
				}
			});
		});
	}
	
	public static void removeNpc(NPC npc) {
		getLocalSegments(npc.getInstanceId(), npc.getDto().getRoamRadius()).forEach(segment -> {
			if (npc.getFloor() < 0) {
				if (!undergroundNpcs.containsKey(npc.getFloor()))
					return;
				
				if (!undergroundNpcs.get(npc.getFloor()).containsKey(segment))
					return;
				
				undergroundNpcs.get(npc.getFloor()).get(segment).remove(npc);
			} else {
				if ((npc.getDto().getAttributes() & NpcAttributes.NOCTURNAL.getValue()) > 0) {
					if (nocturnalNpcs.containsKey(npc.getFloor()) && nocturnalNpcs.get(npc.getFloor()).containsKey(segment))
						nocturnalNpcs.get(npc.getFloor()).get(segment).remove(npc);
				}
				
				if ((npc.getDto().getAttributes() & NpcAttributes.DIURNAL.getValue()) > 0) {
					if (diurnalNpcs.containsKey(npc.getFloor()) && diurnalNpcs.get(npc.getFloor()).containsKey(segment))
						diurnalNpcs.get(npc.getFloor()).get(segment).remove(npc);
				}
			}
		});
	}
	
	public static Set<Player> getLocalPlayers(int floor, int tileId, int radius) {
		Set<Player> localPlayers = new HashSet<>();
		if (!players.containsKey(floor))
			return localPlayers;
		
		final int centreTileX = tileId % PathFinder.LENGTH;
		final int centreTileY = tileId / PathFinder.LENGTH;
		
		getLocalSegments(tileId, radius).forEach(segment -> {
			if (players.get(floor).containsKey(segment)) {
				localPlayers.addAll(players.get(floor).get(segment).stream()
					.filter(player -> {
						final int playerTileX = player.getTileId() % PathFinder.LENGTH;
						final int playerTileY = player.getTileId() / PathFinder.LENGTH;
						
						return Math.abs(centreTileX - playerTileX) <= radius && 
							Math.abs(centreTileY - playerTileY) <= radius;
					}).collect(Collectors.toSet()));
			}
		});
		
		return localPlayers;
	}
	
	public static Set<Player> getLocalPlayersWithinRect(int floor, int topLeftTileId, int bottomRightTileId) {
		Set<Player> localPlayers = new HashSet<>();
		if (!players.containsKey(floor))
			return localPlayers;
		
		final int x1 = topLeftTileId % PathFinder.LENGTH;
		final int x2 = bottomRightTileId % PathFinder.LENGTH;
		
		final int y1 = topLeftTileId / PathFinder.LENGTH;
		final int y2 = bottomRightTileId / PathFinder.LENGTH;
		
		final int rectWidth = x2 - x1;
		final int rectHeight = y2 - y1;
		
		Set<Integer> cornerTileIds = Set.<Integer>of(
			topLeftTileId,
			topLeftTileId + rectWidth,
			topLeftTileId + rectHeight,
			bottomRightTileId
		);
		
		Set<Integer> localSegments = cornerTileIds.stream()
			.map(cornerTileId -> getSegmentFromTileId(cornerTileId)).collect(Collectors.toSet());
		
		
		localSegments.forEach(segment -> {
			if (players.get(floor).containsKey(segment)) {
				localPlayers.addAll(players.get(floor).get(segment).stream()
					.filter(player -> 
						Utils.tileIdWithinRect(player.getTileId(), topLeftTileId, bottomRightTileId)).collect(Collectors.toSet()));
			}
		});
		
		return localPlayers;
	}
	
	public static void addPlayer(Player player) {
		// first check if the player already exists in its current segments
		final Set<Integer> currentSegments = getLocalSegments(player.getTileId(), 12);
		if (players.containsKey(player.getFloor())) {
			boolean containsCurrentSegments = true;
			for (int segment : currentSegments) {
				if (!players.get(player.getFloor()).containsKey(segment) || !players.get(player.getFloor()).get(segment).contains(player)) {
					containsCurrentSegments = false;
					break;
				}
			}

			// the player's current segments are the same, so we don't need to do anything.
			if (containsCurrentSegments)
				return;
		}
		
		// the player's segments have changed, so remove and reset them.
		removePlayerIfExists(player);
		currentSegments.forEach(currentSegment -> {
			players.putIfAbsent(player.getFloor(), new HashMap<>());
			players.get(player.getFloor()).putIfAbsent(currentSegment, new HashSet<>());
			players.get(player.getFloor()).get(currentSegment).add(player);
		});
	}

	public static void removePlayerIfExists(Player player) {
		players.forEach((floor, segmentMap) -> {
			segmentMap.forEach((segment, playerList) -> playerList.removeIf(e -> e.equals(player)));
			segmentMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
		});
		players.entrySet().removeIf(entry -> entry.getValue().isEmpty());
	}
	
	public static void addPet(NPC pet) {
		// first check if the pet already exists in its current segments
		final Set<Integer> currentSegments = getLocalSegments(pet.getTileId(), 12);
		if (pets.containsKey(pet.getFloor())) {
			boolean containsCurrentSegments = true;
			for (int segment : currentSegments) {
				if (!pets.get(pet.getFloor()).containsKey(segment) || !pets.get(pet.getFloor()).get(segment).contains(pet)) {
					containsCurrentSegments = false;
					break;
				}
			}

			// the pet's current segments are the same, so we don't need to do anything.
			if (containsCurrentSegments)
				return;
		}
		
		// the pet's segments have changed, so remove and reset them.
		removePetIfExists(pet);
		currentSegments.forEach(currentSegment -> {
			pets.putIfAbsent(pet.getFloor(), new HashMap<>());
			pets.get(pet.getFloor()).putIfAbsent(currentSegment, new HashSet<>());
			pets.get(pet.getFloor()).get(currentSegment).add(pet);
		});
	}
	
	public static void removePetIfExists(NPC pet) {
		pets.forEach((floor, segmentMap) -> {
			segmentMap.forEach((segment, playerList) -> playerList.removeIf(e -> e.equals(pet)));
			segmentMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
		});
		pets.entrySet().removeIf(entry -> entry.getValue().isEmpty());
	}
	
	public static NPC getPetByFloorAndInstanceId(int floor, int instanceId) {
		if (!pets.containsKey(floor))
			return null;
		
		for (Map.Entry<Integer, Set<NPC>> entry : pets.get(floor).entrySet()) {
			final NPC pet = entry.getValue().stream().filter(e -> e.getInstanceId() == instanceId).findFirst().orElse(null);
			if (pet != null)
				return pet;
		}
		
		return null;
	}
	
	private static Set<Integer> getCornerTileIds(int centreTileId, int radius) {
		return Set.<Integer>of(
				centreTileId - radius - (radius * PathFinder.LENGTH), // top left
				centreTileId + radius - (radius * PathFinder.LENGTH), // top right
				centreTileId - radius + (radius * PathFinder.LENGTH), // bottom left
				centreTileId + radius + (radius * PathFinder.LENGTH)); // bottom right
	}
	
	private static Set<Integer> getLocalSegments(int centreTileId, int radius) {
		return getCornerTileIds(centreTileId, radius).stream()
				.map(cornerTileId -> getSegmentFromTileId(cornerTileId)).collect(Collectors.toSet());
	}
	
	private static int getSegmentFromTileId(int tileId) {
		final int tileX = tileId % PathFinder.LENGTH;
		final int tileY = tileId / PathFinder.LENGTH;
		final int segmentX = tileX / SEGMENT_SIZE;
		final int segmentY = tileY / SEGMENT_SIZE;
		return (segmentY * SEGMENTS_PER_ROW) + segmentX;
	}
}

