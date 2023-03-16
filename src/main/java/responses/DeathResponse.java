package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

public class DeathResponse extends Response {
	@Setter private int id;

	public DeathResponse(int id) {
		this.id = id;
		setAction("dead");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true; // imagine if death was stopped by being in combat
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {

	}

}
