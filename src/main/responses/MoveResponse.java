package main.responses;

import javax.websocket.Session;

import main.FightManager;
import main.database.DbConnection;
import main.database.PlayerDao;
import main.requests.MoveRequest;
import main.requests.Request;
import main.responses.Response.ResponseType;

public class MoveResponse extends Response {
	private int id;
	private int x;
	private int y;

	public MoveResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {
		// the MoveRequest tells us which square the player wants to move to.
		// we run the A* algorithm and return them a list of points to move to.
		
		if (!(req instanceof MoveRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		MoveRequest moveReq = (MoveRequest)req;
		id = moveReq.getId();
		// x/y should be multiple of 32, plus 16 (i.e. in the middle of a 32x32 tile)
		// it's an int so the division truncates, then the multiplication gets it in the right place
		x = ((moveReq.getX() / 32) * 32) + 16;
		y = ((moveReq.getY() / 32) * 32) + 16;
		
		PlayerDao.setDestinationPosition(moveReq.getId(), x, y);
		
		FightManager.cancelFight(moveReq.getId());
		
		setRecoAndResponseText(1, "");
		return ResponseType.broadcast;
	}

}
