package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

@Setter
public class DamageResponse extends Response {
	
	int id;
	int otherId;
	int damage;

	public DamageResponse() {
		setAction("damage");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {

	}

}
