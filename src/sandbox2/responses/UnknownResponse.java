package sandbox2.responses;

import sandbox2.requests.Request;

public class UnknownResponse extends Response {

	public UnknownResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req) {
		setRecoAndResponseText(0, "Unknown action.");
	}
}
