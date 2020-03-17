package main.responses;

import java.util.Map;

import lombok.Setter;
import main.database.AnimationDto;
import main.processing.Player;
import main.requests.Request;
import main.types.PlayerPartType;

// TODO deprecate in favour of PlayerUpdateResponse
public class PlayerEnterResponse extends Response {
	
	@Setter private int id;
	@Setter private String name;
	@Setter private int tileId;
	@Setter private int roomId;
	@Setter private int combatLevel;
	@Setter private int maxHp;
	@Setter private int currentHp;
	@Setter private Map<PlayerPartType, AnimationDto> baseAnimations;
	@Setter private Map<PlayerPartType, AnimationDto> equipAnimations;

	public PlayerEnterResponse() {
		setAction("playerEnter");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
}
