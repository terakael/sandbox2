package main.responses;

import lombok.Getter;
import main.requests.Request;

public class PlayerLeaveResponse extends Response {
	
	@Getter private String userId;
	
	public PlayerLeaveResponse(String action) {
		super(action);
	}

	@Override
	public boolean process(Request req) {
		return false;
	}

}
