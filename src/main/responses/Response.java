package main.responses;

import javax.websocket.Session;

import com.google.gson.Gson;

import lombok.Getter;
import main.requests.Request;

@SuppressWarnings("unused")
public abstract class Response {
	protected static Gson gson = new Gson();
	
	public enum ResponseType {
		broadcast,
		client_only,
		local,
		no_response
	};
	
	@Getter
	private int success = 1;
	
	private String responseText = "";
	private String action;
	
	public Response(String action) {
		this.action = action;
	}

	public void setRecoAndResponseText(int success, String responseText) {
		this.success = success;
		this.responseText = responseText;
	}

	public abstract void process(Request req, Session client, ResponseMaps responseMaps);

}
