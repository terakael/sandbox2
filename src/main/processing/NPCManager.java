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
		if (NPCDao.getNpcList() == null)
			NPCDao.setupCaches();
		
		for (NPCDto dto : NPCDao.getNpcList()) {
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
}
