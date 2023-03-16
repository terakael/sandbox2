package responses;

import processing.attackable.Player;
import requests.LogoffRequest;
import requests.Request;

public class LogoffResponse extends Response {

	public LogoffResponse() {
		setAction("logoff");
		setCombatInterrupt(false);
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		// TODO this needs special handling, like being unable to log out for X seconds after combat
		return super.handleCombat(req, player, responseMaps);
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof LogoffRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}
		
		// everyone should receive the logoff response so the client can remove the otherPlayer (or handle current player logoff)
		responseMaps.addBroadcastResponse(this);
	}

}
