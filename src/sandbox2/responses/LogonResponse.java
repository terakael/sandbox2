package sandbox2.responses;

import sandbox2.Stats;
import sandbox2.requests.Request;

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
