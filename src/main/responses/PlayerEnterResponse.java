package main.responses;

import javax.websocket.Session;

import lombok.Setter;
import main.database.PlayerDto;
import main.requests.Request;

// TODO deprecate in favour of PlayerUpdateResponse
public class PlayerEnterResponse extends Response {
	
	@Setter private String id;
	@Setter private String name;
	@Setter private int tileId;
	
	PlayerDto player;

	public PlayerEnterResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		
	}
	
	public void setPlayer(PlayerDto player) {
		this.player = player;
	}

}
