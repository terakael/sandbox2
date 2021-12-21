package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

@Setter
@SuppressWarnings("unused")
public class PvpEndResponse extends Response {
	private int player1Id;
	private int player2Id;
	private int player1TileId;
	private int player2TileId;
	
	public PvpEndResponse() {
		setAction("pvp_end");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
