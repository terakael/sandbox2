package main.responses;

import java.util.List;

import main.processing.NPC;
import main.processing.NPCManager;
import main.processing.Player;
import main.requests.Request;

public class NpcFullUpdateResponse extends Response {
	List<NPC> npcs;
	
	public NpcFullUpdateResponse() {
		setAction("npc_full_update");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		npcs = NPCManager.get().getNpcs();
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
