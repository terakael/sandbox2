package main.responses;

import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import lombok.Getter;
import lombok.Setter;
import main.requests.Request;
import main.state.Player;

public class PlayerLeaveResponse extends Response {
	
	@Getter @Setter private int id;
	@Setter private String name;
	
	public PlayerLeaveResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		return ResponseType.client_only;
	}

}
