package responses;

import java.util.Set;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

public class NpcOutOfRangeResponse extends Response {
	
	@Setter private Set<Integer> instances;
	
	public NpcOutOfRangeResponse() {
		setAction("npc_out_of_range");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
