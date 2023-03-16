package responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Setter;
import processing.attackable.NPC;
import processing.attackable.Player;
import requests.Request;

@Setter
@SuppressWarnings("unused")
public class NpcInRangeResponse extends Response {
	@AllArgsConstructor
	public static class NpcLocation {
		private int npcId;
		private int instanceId;
		private int tileId;
		private int currentHp;
		private int ownerId; // for pets
	}
	private List<NpcLocation> npcs = new ArrayList<>();
	
	public NpcInRangeResponse() {
		setAction("npc_in_range");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}
	
	public void addInstances(Set<NPC> instances) {
		instances.forEach(npc -> npcs.add(new NpcLocation(npc.getId(), npc.getInstanceId(), npc.getTileId(), npc.getCurrentHp(), npc.getOwnerId())));
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
