package main.responses;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

@Setter
public class PvmEndResponse extends Response {
	private int playerId;
	private int monsterId;
	private Integer playerTileId;
	private Integer monsterTileId;
	
	public PvmEndResponse() {
		setAction("pvm_end");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
