package responses;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

public class TeleportExplosionResponse extends Response {
	@Setter private int tileId;
	
	public TeleportExplosionResponse(int tileId) {
		setAction("teleport_explosion");
		this.tileId = tileId;
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
	}
}
