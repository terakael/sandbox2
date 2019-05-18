package main.responses;

import java.util.Stack;

import javax.websocket.Session;

import main.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.processing.Player.PlayerState;
import main.requests.MoveRequest;
import main.requests.Request;

public class MoveResponse extends Response {
	public MoveResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		// the MoveRequest tells us which square the player wants to move to.
		// we run the A* algorithm and return them a list of points to move to.
		if (!(req instanceof MoveRequest))
			return;
		
		MoveRequest moveReq = (MoveRequest)req;

		FightManager.cancelFight(moveReq.getId());
		
		Player player = WorldProcessor.playerSessions.get(client);
		if (player != null) {
			player.setState(PlayerState.walking);
			
			int destX = moveReq.getX() / 32;
			int destY = moveReq.getY() / 32;
			
			int destTile = destX + (destY * 250);
			
			Stack<Integer> ints = PathFinder.findPath(player.getTileId(), destTile, true);
			player.setPath(ints);
		}
	}

}
