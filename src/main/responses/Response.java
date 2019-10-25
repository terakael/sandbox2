package main.responses;

import javax.websocket.Session;

import com.google.gson.Gson;

import lombok.Getter;
import lombok.Setter;
import main.processing.Player;
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
	
	@Setter private String responseText = "";
	@Getter @Setter private String action;
	@Setter private String colour = null;

	public void setRecoAndResponseText(int success, String responseText) {
		this.success = success;
		this.responseText = responseText;
	}

	public abstract void process(Request req, Player player, ResponseMaps responseMaps);

}
