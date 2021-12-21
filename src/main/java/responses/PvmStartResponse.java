package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

@Setter
@SuppressWarnings("unused")
public class PvmStartResponse extends Response {
	private int playerId;
	private int monsterId;
	private int tileId;
	
	public PvmStartResponse() {
		setAction("pvm_start");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
