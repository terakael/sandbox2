package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

@Setter
public class DamageResponse extends Response {
	
	int tileId;
	int damage;

	public DamageResponse() {
		setAction("damage");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {

	}

}
