package main.responses;

import javax.websocket.Session;

import main.requests.LogoffRequest;
import main.requests.Request;

public class LogoffResponse extends Response {

	public LogoffResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof LogoffRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}
		
		// everyone should receive the logoff response so the client can remove the otherPlayer (or handle current player logoff)
		responseMaps.addBroadcastResponse(this);
	}

}
