package main.responses;

import java.util.HashMap;
import java.util.Map;

import lombok.Setter;
import main.database.AnimationDto;
import main.processing.Player;
import main.requests.Request;
import main.types.PlayerPartType;


public class PlayerUpdateResponse extends Response {
	@Setter Integer id = null;
	@Setter String name = null;
	@Setter Integer cmb = null;
	@Setter Integer tile = null;
	@Setter String state = null;
	@Setter Integer damage = null;
	@Setter Integer hp = null;
	@Setter private Map<PlayerPartType, AnimationDto> equipAnimations = null;

	public PlayerUpdateResponse() {
		setAction("player_update");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {

	}

}
