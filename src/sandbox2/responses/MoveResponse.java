package sandbox2.responses;

import sandbox2.requests.Request;

public class MoveResponse extends Response {

	public MoveResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req) {
		// the MoveRequest tells us which square the player wants to move to.
		// we run the A* algorithm and return them a list of points to move to.
	}

}
