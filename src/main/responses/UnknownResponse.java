package main.responses;

import main.requests.Request;

public class UnknownResponse extends Response {

	public UnknownResponse(String action) {
		super(action);
	}

	@Override
	public boolean process(Request req) {
		assert(false);
		setRecoAndResponseText(0, "Unknown action.");
		return false;
	}
}
