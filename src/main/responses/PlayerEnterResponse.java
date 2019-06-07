package main.responses;

import lombok.Setter;
import main.database.AnimationDto;
import main.processing.Player;
import main.requests.Request;

// TODO deprecate in favour of PlayerUpdateResponse
public class PlayerEnterResponse extends Response {
	
	@Setter private int id;
	@Setter private String name;
	@Setter private int tileId;
	@Setter private int combatLevel;
	@Setter private int maxHp;
	@Setter private int currentHp;
	@Setter private AnimationDto animations;

	public PlayerEnterResponse() {
		setAction("playerEnter");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
}
