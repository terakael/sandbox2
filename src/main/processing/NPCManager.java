package main.processing;

import java.util.ArrayList;

import lombok.Getter;
import main.database.NPCDao;
import main.database.NPCDto;
import main.responses.ResponseMaps;

public class NPCManager {
	private NPCManager() {};
	
	private static NPCManager instance;
	@Getter private ArrayList<NPC> npcs = new ArrayList<>();
	
	public static NPCManager get() {
		if (instance == null)
			instance = new NPCManager();
		return instance;
	}
	
	public void loadNpcs() {
		if (NPCDao.getNpcInstanceList() == null)
			NPCDao.setupCaches();
		
		for (NPCDto dto : NPCDao.getNpcInstanceList()) {
			npcs.add(new NPC(dto));
		}
	}
	
	// TODO should probs be a Map<id, NPC>
	public NPC getNpcByInstanceId(int id) {
		for (NPC npc : npcs) {
			if (npc.getDto().getTileId() == id)// tileId is the instance id
				return npc;
		}
		return null;
	}
	
	public void process(ResponseMaps responseMaps) {
		for (NPC npc : npcs) {
			npc.process(responseMaps);
		}
	}
	
	public ArrayList<NPC> getNpcsNearTile(int tileId, int radius) {
		final int tileX = tileId % PathFinder.LENGTH;
		final int tileY = tileId / PathFinder.LENGTH;
		
		final int minX = tileX - radius;
		final int maxX = tileX + radius;
		final int minY = tileY - radius;
		final int maxY = tileY + radius;
		ArrayList<NPC> localNpcs = new ArrayList<>();
		for (NPC npc : npcs) {
			if (npc.isDead())
				continue;
			
			final int npcTileX = npc.getTileId() % PathFinder.LENGTH;
			final int npcTileY = npc.getTileId() / PathFinder.LENGTH;
			
			if (npcTileX >= minX && npcTileX <= maxX && npcTileY >= minY && npcTileY <= maxY)
				localNpcs.add(npc);
		}
		
		return localNpcs;
	}
}
