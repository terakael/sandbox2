package main.responses;

import java.util.HashSet;
import java.util.Set;

import lombok.Setter;
import main.processing.attackable.Player;
import main.requests.Request;

public class PlayerOutOfRangeResponse extends Response {
	@Setter private Set<Integer> playerIds = new HashSet<>();
	
	public PlayerOutOfRangeResponse() {
		setAction("player_out_of_range");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
