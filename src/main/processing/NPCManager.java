package main.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import main.database.NPCDao;
import main.database.NPCDto;
import main.responses.ResponseMaps;

public class NPCManager {
	private NPCManager() {};
	
	private static NPCManager instance;
	@Getter private HashMap<Integer, ArrayList<NPC>> npcs = new HashMap<>();
	
	public static NPCManager get() {
		if (instance == null)
			instance = new NPCManager();
		return instance;
	}
	
	public void loadNpcs() {
		if (NPCDao.getNpcInstanceList() == null)
			NPCDao.setupCaches();
		
		for (Map.Entry<Integer, ArrayList<NPCDto>> entry : NPCDao.getNpcInstanceList().entrySet()) {
			npcs.put(entry.getKey(), new ArrayList<>());
			for (NPCDto dto : entry.getValue()) {
				npcs.get(entry.getKey()).add(new NPC(dto));
			}
		}
		
		
	}
	
	// TODO should probs be a Map<id, NPC>
	public NPC getNpcByInstanceId(int roomId, int id) {
		if (!npcs.containsKey(roomId))
			return null;
		
		for (NPC npc : npcs.get(roomId)) {
			if (npc.getDto().getTileId() == id)// tileId is the instance id
				return npc;
		}
		return null;
	}
	
	public void process(ResponseMaps responseMaps) {
		for (Map.Entry<Integer, ArrayList<NPC>> entry : npcs.entrySet()) {
			for (NPC npc : entry.getValue()) {
				npc.process(responseMaps);
			}
		}
	}
	
	public ArrayList<NPC> getNpcsNearTile(int roomId, int tileId, int radius) {
		if (!npcs.containsKey(roomId))
			return new ArrayList<>();// if there's no room then there's no NPCs so return an empty list
		
		final int tileX = tileId % PathFinder.LENGTH;
		final int tileY = tileId / PathFinder.LENGTH;
		
		final int minX = tileX - radius;
		final int maxX = tileX + radius;
		final int minY = tileY - radius;
		final int maxY = tileY + radius;
		ArrayList<NPC> localNpcs = new ArrayList<>();
		for (NPC npc : npcs.get(roomId)) {			
			final int npcTileX = npc.getTileId() % PathFinder.LENGTH;
			final int npcTileY = npc.getTileId() / PathFinder.LENGTH;
			
			if (npcTileX >= minX && npcTileX <= maxX && npcTileY >= minY && npcTileY <= maxY)
				localNpcs.add(npc);
		}
		
		return localNpcs;
	}
}
