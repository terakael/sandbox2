package main.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import main.database.dao.NPCDao;
import main.database.dto.NPCDto;
import main.responses.ResponseMaps;

public class NPCManager {
	private NPCManager() {};
	
	private static NPCManager instance;
	@Getter private Map<Integer, List<NPC>> npcs = new HashMap<>();
	
	public static NPCManager get() {
		if (instance == null)
			instance = new NPCManager();
		return instance;
	}
	
	public void loadNpcs() {
		if (NPCDao.getNpcInstanceList() == null)
			NPCDao.setupCaches();
		
		for (Map.Entry<Integer, List<NPCDto>> entry : NPCDao.getNpcInstanceList().entrySet()) {
			npcs.put(entry.getKey(), new ArrayList<>());
			for (NPCDto dto : entry.getValue()) {
				npcs.get(entry.getKey()).add(new NPC(dto));
			}
		}
		
		LocationManager.addNpcs(npcs.values().stream().flatMap(List::stream).collect(Collectors.toList()));
	}
	
	// TODO should probs be a Map<id, NPC>
	public NPC getNpcByInstanceId(int floor, int id) {
		if (!npcs.containsKey(floor))
			return null;
		
		for (NPC npc : npcs.get(floor)) {
			if (npc.getDto().getTileId() == id)// tileId is the instance id
				return npc;
		}
		return null;
	}
	
	public void process(ResponseMaps responseMaps, int tick) {
		LocationManager.getAllNpcsNearPlayers(WorldProcessor.isDaytime()).forEach((floor, npcSet) -> {
			npcSet.forEach(npc -> npc.process(tick, responseMaps));
		});
	}
	
	public List<NPC> getNpcsNearTile(int floor, int tileId, int radius) {
		if (!npcs.containsKey(floor))
			return new ArrayList<>();// if there's no room then there's no NPCs so return an empty list
		
		final int tileX = tileId % PathFinder.LENGTH;
		final int tileY = tileId / PathFinder.LENGTH;
		
		final int minX = tileX - radius;
		final int maxX = tileX + radius;
		final int minY = tileY - radius;
		final int maxY = tileY + radius;
		List<NPC> localNpcs = new ArrayList<>();
		for (NPC npc : npcs.get(floor)) {			
			final int npcTileX = npc.getTileId() % PathFinder.LENGTH;
			final int npcTileY = npc.getTileId() / PathFinder.LENGTH;
			
			if (npcTileX >= minX && npcTileX <= maxX && npcTileY >= minY && npcTileY <= maxY)
				localNpcs.add(npc);
		}
		
		return localNpcs;
	}
}
