package main.responses;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class NpcLocationRefreshResponse extends Response {	
	@AllArgsConstructor
	public static class NpcLocation {
		private int npcId;
		private int instanceId;
		private int tileId;
	}
	
	@Setter private ArrayList<NpcLocation> npcs = new ArrayList<>();
	
	public NpcLocationRefreshResponse() {
		setAction("npc_location_refresh");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

//	public void add(int npcId, int instanceId, int tileId) {
//		npcs.add(new NpcLocation(npcId, instanceId, tileId));
//	}
}
