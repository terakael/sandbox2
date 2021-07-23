package responses;

import java.util.Stack;

import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import processing.managers.FightManager.Fight;
import requests.MoveRequest;
import requests.Request;
import types.DuelRules;

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
			Fight fight = FightManager.getFightByPlayerId(player.getId());
			if (fight.getRules() != null && (fight.getRules() & DuelRules.no_retreat.getValue()) > 0) {
				setRecoAndResponseText(0, "you can't retreat in this duel!");
			} else {
				setRecoAndResponseText(0, "you can't retreat yet!");
			}
			
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}

		FightManager.cancelFight(player, responseMaps);
		
		if (player != null) {
			player.setState(PlayerState.walking);
			player.setSavedRequest(null);
			
			int destX = moveReq.getX() / 32;
			int destY = moveReq.getY() / 32;
			
			int destTile = destX + (destY * PathFinder.LENGTH);
			
			Stack<Integer> ints = PathFinder.findPath(player.getFloor(), player.getTileId(), destTile, true);
			player.setPath(ints);
		}
	}

}
