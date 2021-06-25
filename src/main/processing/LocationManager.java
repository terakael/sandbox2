package main.processing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LocationManager {
	private static final int SEGMENT_SIZE = 25;
	private static final int SEGMENTS_PER_ROW = PathFinder.LENGTH / SEGMENT_SIZE;
	
	private static Map<Integer, Map<Integer, Set<NPC>>> npcs = new HashMap<>(); // floor, <segment, <npc>>
	private static Map<Integer, Map<Integer, Set<Player>>> players = new HashMap<>();
	
	public static Set<NPC> getLocalNpcs(int floor, int tileId, int radius) {
		Set<NPC> localNpcs = new HashSet<>();
		if (!npcs.containsKey(floor))
			return localNpcs;
		
		final int playerTileIdX = tileId % PathFinder.LENGTH;
		final int playerTileIdY = tileId / PathFinder.LENGTH;
		
		getLocalSegments(tileId, radius).forEach(segment -> {
			if (npcs.get(floor).containsKey(segment)) {
				localNpcs.addAll(npcs.get(floor).get(segment).stream()
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
	
	public static void addNpcs(List<NPC> allNpcs) {
		allNpcs.forEach(npc -> {
			// local segments are the one that the spawn tile lies in, plus sibling segments if the roam radius intersects.
			// this means if a spawn tile is on the corner of a segment, there will be four segments here as the 
			// roam range will intersect all four.
			getLocalSegments(npc.getInstanceId(), npc.getDto().getRoamRadius()).forEach(segment -> {
				npcs.putIfAbsent(npc.getFloor(), new HashMap<>());
				npcs.get(npc.getFloor()).putIfAbsent(segment, new HashSet<>());
				npcs.get(npc.getFloor()).get(segment).add(npc);
			});
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
	
	public static void addPlayer(Player player) {
		int currentSegment = getSegmentFromTileId(player.getTileId());
		
		players.putIfAbsent(player.getFloor(), new HashMap<>());
		players.get(player.getFloor()).putIfAbsent(currentSegment, new HashSet<>());
		
		// if the player already exists in this segment then we don't need to do anything.
		if (!players.get(player.getFloor()).get(currentSegment).contains(player)) {
			// the player might exist in another segment (e.g. crossed a segment border, went up a ladder, teleported).
			// if that's the case then remove the player from the other segment.
			removePlayerIfExists(player);
			players.get(player.getFloor()).get(currentSegment).add(player);
		}
	}
	
	public static void removePlayerIfExists(Player player) {
		players.forEach((floor, segmentMap) -> {
			segmentMap.forEach((segment, playerList) -> {
				if (playerList.contains(player))
					playerList.remove(player);
				
				if (playerList.isEmpty())
					segmentMap.remove(segment);
				
				return;
			});
		});
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

