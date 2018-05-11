package main.responses;

import main.requests.LogoffRequest;
import main.requests.Request;

public class LogoffResponse extends Response {

	public LogoffResponse(String action) {
		super(action);
	}

	@Override
	public boolean process(Request req) {
		if (!(req instanceof LogoffRequest)) {
			setRecoAndResponseText(0, "funny business");
			return false;
		}
		
		return false;
	}

}
