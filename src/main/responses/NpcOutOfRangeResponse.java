package main.responses;

import java.util.Set;

import lombok.Setter;
import main.processing.attackable.Player;
import main.requests.Request;

public class NpcOutOfRangeResponse extends Response {
	
	@Setter private Set<Integer> instances;
	
	public NpcOutOfRangeResponse() {
		setAction("npc_out_of_range");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
