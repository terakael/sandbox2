package main.responses;

import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;

import javax.websocket.Session;

import main.FightManager;
import main.database.DbConnection;
import main.database.PlayerDao;
import main.processing.PathFinder;
import main.processing.WorldProcessor;
import main.requests.MoveRequest;
import main.requests.Request;
import main.responses.Response.ResponseType;
import main.state.Player;
import main.state.Player.PlayerState;

public class MoveResponse extends Response {
	private int id;
	private int x;
	private int y;

	public MoveResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		// the MoveRequest tells us which square the player wants to move to.
		// we run the A* algorithm and return them a list of points to move to.
		
		if (!(req instanceof MoveRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		MoveRequest moveReq = (MoveRequest)req;

		FightManager.cancelFight(moveReq.getId());
		
		Player player = WorldProcessor.playerSessions.get(client);
		if (player != null) {
			player.setState(PlayerState.walking);
			
			int destX = moveReq.getX() / 32;
			int destY = moveReq.getY() / 32;
			
			
			int srcTile = player.getTileId();
			int destTile = destX + (destY * 250);
			
			Stack<Integer> ints = PathFinder.findPath(srcTile, destTile, true);
			player.setPath(ints);
		}
		
		setRecoAndResponseText(1, "");
		responseMaps.addBroadcastResponse(this);
		return ResponseType.broadcast;
	}

}
