package main.responses;

import javax.websocket.Session;

import main.processing.WorldProcessor;
import main.requests.MineRequest;
import main.requests.Request;

public class StartMiningResponse extends Response {

	public StartMiningResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof MineRequest))
			return;
		
		MineRequest request = (MineRequest)req;
		responseMaps.addClientOnlyResponse(WorldProcessor.playerSessions.get(client), this);
	}

}
