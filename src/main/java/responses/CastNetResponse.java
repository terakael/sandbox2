package responses;

import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.attackable.Ship;
import processing.managers.ShipManager;
import requests.Request;

public class CastNetResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		final Ship ship = ShipManager.getShipWithPlayer(player);
		if (ship == null) {
			setRecoAndResponseText(0, "you need to be onboard to cast the net.");
			responseMaps.addClientOnlyResponse(player, this);
		}
		
		setRecoAndResponseText(1, "you cast out the net...");
		responseMaps.addClientOnlyResponse(player, this);
		
		player.setState(PlayerState.casting_net);
	}

}
