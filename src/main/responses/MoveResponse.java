package main.responses;

import main.database.DbConnection;
import main.database.PlayerDao;
import main.requests.MoveRequest;
import main.requests.Request;

public class MoveResponse extends Response {
	private int id;
	private int x;
	private int y;

	public MoveResponse(String action) {
		super(action);
	}

	@Override
	public boolean process(Request req) {
		// the MoveRequest tells us which square the player wants to move to.
		// we run the A* algorithm and return them a list of points to move to.
		
		if (!(req instanceof MoveRequest)) {
			setRecoAndResponseText(0, "funny business");
			return false;
		}
		
		MoveRequest moveReq = (MoveRequest)req;
		PlayerDao dao = new PlayerDao(DbConnection.get());
		id = moveReq.getId();
		// x/y should be multiple of 32, plus 16 (i.e. in the middle of a 32x32 tile)
		// it's an int so the division truncates, then the multiplication gets it in the right place
		x = ((moveReq.getX() / 32) * 32) + 16;
		y = ((moveReq.getY() / 32) * 32) + 16;
		
		dao.setDestinationPosition(moveReq.getId(), x, y);
		
		setRecoAndResponseText(1, "");
		return true;
	}

}
