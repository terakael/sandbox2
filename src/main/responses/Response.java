package main.responses;

import com.google.gson.Gson;

import lombok.Getter;
import lombok.Setter;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.MessageRequest;
import main.requests.Request;

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
	
	public final void processSuper(Request req, Player player, ResponseMaps responseMaps) {
		// message request is a special case as it's the only action that doesn't directly affect the player
		if (!(req instanceof MessageRequest) && player.getState() == PlayerState.dead)
			return;
		
		process(req, player, responseMaps);
	}

	public abstract void process(Request req, Player player, ResponseMaps responseMaps);

}
