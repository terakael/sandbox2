package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

@Setter
public class PvmStartResponse extends Response {
	private int playerId;
	private int monsterId;
	private int tileId;
	
	public PvmStartResponse() {
		setAction("pvm_start");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
