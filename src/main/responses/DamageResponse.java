package main.responses;

import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import lombok.Setter;
import main.requests.Request;
import main.state.Player;

@Setter
public class DamageResponse extends Response {
	
	int id;
	int otherId;
	int damage;

	public DamageResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		return null;
	}

}
