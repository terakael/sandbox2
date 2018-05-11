package main.responses;

import lombok.Setter;
import main.database.PlayerDto;
import main.requests.Request;

public class PlayerEnterResponse extends Response {
	
	@Setter private String userId;
	@Setter private String username;
	@Setter private int x;
	@Setter private int y;
	
	PlayerDto player;

	public PlayerEnterResponse(String action) {
		super(action);
	}

	@Override
	public boolean process(Request req) {
		return false;
	}
	
	public void setPlayer(PlayerDto player) {
		this.player = player;
	}

}
