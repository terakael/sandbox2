package main.responses;

import main.processing.Player;
import main.requests.Request;

public class ActionBubbleResponse extends Response {
	private int playerId;
	private int iconId;
	
	public ActionBubbleResponse(int playerId, int iconId) {
		setAction("action_bubble");
		this.playerId = playerId;
		this.iconId = iconId;
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
}
