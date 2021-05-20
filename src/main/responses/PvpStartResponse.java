package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

@Setter
@SuppressWarnings("unused")
public class PvpStartResponse extends Response {
	private int player1Id;
	private int player2Id;
	private int tileId;
	
	public PvpStartResponse() {
		setAction("pvp_start");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
