package responses;

import java.util.Map;

import processing.attackable.Player;
import requests.Request;

public class ShowDialogueOptionsResponse extends Response {
	private Map<Integer, String> options;
	
	public ShowDialogueOptionsResponse(Map<Integer, String> options) {
		setAction("show_dialogue_options");
		this.options = options;
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		responseMaps.addClientOnlyResponse(player, this);
	}
}
