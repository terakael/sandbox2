package main.responses;

import javax.websocket.Session;

import main.requests.Request;

public class UnknownResponse extends Response {

	public UnknownResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {
		assert(false);
		setRecoAndResponseText(0, "Unknown action.");
		return ResponseType.client_only;
	}
}
