package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

@Setter
@SuppressWarnings("unused")
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
