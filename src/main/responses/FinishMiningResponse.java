package main.responses;

import javax.websocket.Session;

import main.processing.WorldProcessor;
import main.requests.MineRequest;
import main.requests.Request;

public class FinishMiningResponse extends Response{
	private int tileId;

	public FinishMiningResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof MineRequest))
			return;
		
		MineRequest request = (MineRequest)req;
		tileId = request.getTileId();// the tile we just finished mining
		responseMaps.addClientOnlyResponse(WorldProcessor.playerSessions.get(client), this);
	}

}
