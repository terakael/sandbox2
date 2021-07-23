package responses;

import java.util.Map;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

@Setter
@SuppressWarnings("unused")
public class AcceptTradeResponse extends Response {
	private int otherPlayerId;
	private Map<Integer, String> duelRules = null;
	
	public AcceptTradeResponse() {
		setAction("accept_trade");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
}
