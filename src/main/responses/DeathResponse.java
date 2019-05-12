package main.responses;

import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;
import main.requests.Request;
import main.state.Player;

public class DeathResponse extends Response {
	@Getter @Setter private int id;
	@Getter @Setter private int tileId;
	@Setter private int currentHp;

	public DeathResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		return null;
	}

}
