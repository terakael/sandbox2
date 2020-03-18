package main.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Setter;
import main.processing.NPC;
import main.processing.NPCManager;
import main.processing.Player;
import main.requests.Request;

@Setter
public class NpcInRangeResponse extends Response {
	@AllArgsConstructor
	public static class NpcLocation {
		private int npcId;
		private int instanceId;
		private int tileId;
		private int currentHp;
	}
	private List<NpcLocation> npcs = new ArrayList<>();
	
	public NpcInRangeResponse() {
		setAction("npc_in_range");
	}

	public void addInstances(int roomId, Set<Integer> instanceIds) {
		for (int instanceId : instanceIds) {
			NPC npc = NPCManager.get().getNpcByInstanceId(roomId, instanceId);
			npcs.add(new NpcLocation(npc.getId(), npc.getInstanceId(), npc.getTileId(), npc.getCurrentHp()));
		}
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
