package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

@Setter
public class AcceptTradeResponse extends Response {
	private int otherPlayerId;
	
	public AcceptTradeResponse() {
		setAction("accept_trade");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
}
