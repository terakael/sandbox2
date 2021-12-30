package builder.responses;

import java.util.List;

import builder.managers.BuildManager;
import builder.requests.Request;

public class MoveResponse extends Response {
	private int tileId;
	private int floor;

	@Override
	public void process(Request req, List<Response> responses) {
		setAction("move");
		
		tileId = req.getTileId();
		floor = req.getFloor();
		
		BuildManager.get().setPosition(floor, tileId);
		
		responses.add(this);
		new LoadInstancesResponse().process(req, responses);
	}

}
