package main.responses;

import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import main.requests.Request;
import main.state.Player;

public class UnknownResponse extends Response {

	public UnknownResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		assert(false);
		setRecoAndResponseText(0, "Unknown action.");
		return ResponseType.client_only;
	}
}
