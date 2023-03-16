package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

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
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
