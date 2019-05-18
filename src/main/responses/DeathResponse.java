package main.responses;

import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;
import main.requests.Request;

public class DeathResponse extends Response {
	@Getter @Setter private int id;
	@Getter @Setter private int tileId;
	@Setter private int currentHp;

	public DeathResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {

	}

}