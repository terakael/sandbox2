package main.responses;

import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import main.processing.WorldProcessor;
import main.requests.LogoffRequest;
import main.requests.Request;
import main.state.Player;

public class LogoffResponse extends Response {

	public LogoffResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof LogoffRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		responseMaps.addClientOnlyResponse(WorldProcessor.playerSessions.get(client), this);
		return ResponseType.client_only;
	}

}
