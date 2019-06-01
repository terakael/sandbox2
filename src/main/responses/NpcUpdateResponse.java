package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class NpcUpdateResponse extends Response {
	@Setter private int instanceId;
	@Setter private int tileId;
	
	public NpcUpdateResponse() {
		setAction("npc_update");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
