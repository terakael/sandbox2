package main.responses;

import javax.websocket.Session;

import main.requests.LogoffRequest;
import main.requests.Request;

public class LogoffResponse extends Response {

	public LogoffResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {
		if (!(req instanceof LogoffRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		return ResponseType.client_only;
	}

}
