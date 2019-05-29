package main.responses;

import lombok.Setter;
import main.database.PlayerDto;
import main.processing.Player;
import main.requests.Request;

// TODO deprecate in favour of PlayerUpdateResponse
public class PlayerEnterResponse extends Response {
	
	@Setter private String id;
	@Setter private String name;
	@Setter private int tileId;
	@Setter private int combatLevel;
	PlayerDto player;

	public PlayerEnterResponse() {
		setAction("playerEnter");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
	public void setPlayer(PlayerDto player) {
		this.player = player;
	}

}
