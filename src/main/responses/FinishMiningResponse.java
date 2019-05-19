package main.responses;

import main.processing.Player;
import main.requests.MineRequest;
import main.requests.Request;

public class FinishMiningResponse extends Response{
	private int tileId;

	public FinishMiningResponse() {
		setAction("finish_mining");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof MineRequest))
			return;
		
		MineRequest request = (MineRequest)req;
		tileId = request.getTileId();// the tile we just finished mining
		responseMaps.addClientOnlyResponse(player, this);
	}

}
