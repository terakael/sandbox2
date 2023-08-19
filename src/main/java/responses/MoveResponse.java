package responses;

import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.attackable.Ship;
import processing.managers.ShipManager;
import requests.MoveRequest;
import requests.Request;

public class MoveResponse extends Response {
	public MoveResponse() {
		setAction("move");
		setCombatLockedMessage("you can't retreat yet!");
		setNoRetreatDuelMessage("you can't retreat in this duel!");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		MoveRequest moveReq = (MoveRequest)req;
		
		final int destTile = (moveReq.getX() / 32) + ((moveReq.getY() / 32) * PathFinder.LENGTH);
		player.setState(PlayerState.walking);
		player.setSavedRequest(null);
		
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), destTile, true));
	}

}
