package main.responses;

import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;
import main.requests.Request;

public class DeathResponse extends Response {
	@Getter @Setter private int id;
	@Getter @Setter private int x;// respawn coordinate
	@Getter @Setter private int y;// respawn coordinate
	@Setter private int currentHp;

	public DeathResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {
		return null;
	}

}
