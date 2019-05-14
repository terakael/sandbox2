package main.responses;

import javax.websocket.Session;

import main.requests.Request;

public class UnknownResponse extends Response {

	public UnknownResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		assert(false);
		setRecoAndResponseText(0, "Unknown action.");
	}
}
