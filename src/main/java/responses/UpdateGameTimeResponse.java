package responses;

import processing.attackable.Player;
import requests.Request;

public class UpdateGameTimeResponse extends Response {
	private String time = "";
	public UpdateGameTimeResponse(String time) {
		setAction("update_game_time");
		this.time = time;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
