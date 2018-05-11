package main.responses;

import lombok.Getter;
import main.requests.Request;

public abstract class Response {
	
	@Getter
	private int success;
	private String responseText;
	private String action;
	
	public Response(String action) {
		this.action = action;
	}

	public void setRecoAndResponseText(int success, String responseText) {
		this.success = success;
		this.responseText = responseText;
	}

	public abstract boolean process(Request req);// true broadcasts message to all, false sends to requester

}
