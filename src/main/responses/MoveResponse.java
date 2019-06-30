package main.responses;

import java.util.Stack;

import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.MoveRequest;
import main.requests.Request;

public class MoveResponse extends Response {
	public MoveResponse() {
		setAction("move");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// the MoveRequest tells us which square the player wants to move to.
		// we run the A* algorithm and return them a list of points to move to.
		if (!(req instanceof MoveRequest))
			return;
		
		MoveRequest moveReq = (MoveRequest)req;
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't retreat yet!");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}

		FightManager.cancelFight(player, responseMaps);
		
		if (player != null) {
			player.setState(PlayerState.walking);
			player.setSavedRequest(null);
			
			int destX = moveReq.getX() / 32;
			int destY = moveReq.getY() / 32;
			
			int destTile = destX + (destY * 250);
			
			Stack<Integer> ints = PathFinder.findPath(player.getTileId(), destTile, true);
			player.setPath(ints);
		}
	}

}
