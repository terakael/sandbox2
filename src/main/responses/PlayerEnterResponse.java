package main.responses;

import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import lombok.Setter;
import main.database.AnimationDto;
import main.database.PlayerDto;
import main.requests.Request;
import main.responses.Response.ResponseType;
import main.state.Player;

public class PlayerEnterResponse extends Response {
	
	@Setter private String id;
	@Setter private String name;
	@Setter private int tileId;
	
	PlayerDto player;

	public PlayerEnterResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		return ResponseType.client_only;
	}
	
	public void setPlayer(PlayerDto player) {
		this.player = player;
	}

}
