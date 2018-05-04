package main.responses;

import main.requests.Request;

public abstract class Response {
	
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

	public abstract void process(Request req);

}
