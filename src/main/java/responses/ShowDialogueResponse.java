package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

@Setter
public class ShowDialogueResponse extends Response {
	private String dialogue = "";
	private String speaker = "";
	
	public ShowDialogueResponse() {
		setAction("show_dialogue");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
