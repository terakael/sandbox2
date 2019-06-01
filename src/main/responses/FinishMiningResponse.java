package main.responses;

import main.database.ItemDao;
import main.database.ItemDto;
import main.database.MineableDao;
import main.database.MineableDto;
import main.processing.Player;
import main.requests.MineRequest;
import main.requests.Request;

public class FinishMiningResponse extends Response {
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
		
		// get the thing we just mined based off the tile, then send a message saying we got the ore from it.
		MineableDto mineable = MineableDao.getMineableDtoByTileId(tileId);
		if (mineable != null)
			setResponseText(String.format("you mine some %s.", ItemDao.getNameFromId(mineable.getItemId())));
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
