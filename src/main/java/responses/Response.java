package responses;

import lombok.Getter;
import lombok.Setter;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import requests.MessageRequest;
import requests.Request;

public abstract class Response {
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
		
		// TODO check if in fight
		// TODO check if next to
		
		process(req, player, responseMaps);
	}

	public abstract void process(Request req, Player player, ResponseMaps responseMaps);

}