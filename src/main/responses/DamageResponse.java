package main.responses;

import javax.websocket.Session;

import lombok.Setter;
import main.requests.Request;

@Setter
public class DamageResponse extends Response {
	
	int id;
	int otherId;
	int damage;

	public DamageResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {

	}

}
