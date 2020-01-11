package main.responses;

import java.util.HashSet;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class NpcOutOfRangeResponse extends Response {
	
	@Setter private HashSet<Integer> instances;
	
	public NpcOutOfRangeResponse() {
		setAction("npc_out_of_range");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
