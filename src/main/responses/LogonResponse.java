package main.responses;

import main.Stats;
import main.requests.Request;

public class LogonResponse extends Response {
	
	private String userId;
	private String username;
	private Stats stats;

	public LogonResponse(String action) {
		super(action);
	}
	
	@Override
	public void process(Request req) {
		
		userId = "1";
		username = "dmk";
		stats = new Stats();

		setRecoAndResponseText(1, "");
	}
}
