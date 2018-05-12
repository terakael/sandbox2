package main.responses;

import javax.websocket.Session;

import lombok.Setter;
import main.database.PlayerDto;
import main.requests.Request;
import main.responses.Response.ResponseType;

public class PlayerEnterResponse extends Response {
	
	@Setter private String id;
	@Setter private String name;
	@Setter private int x;
	@Setter private int y;
	
	PlayerDto player;

	public PlayerEnterResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {
		return ResponseType.client_only;
	}
	
	public void setPlayer(PlayerDto player) {
		this.player = player;
	}

}
