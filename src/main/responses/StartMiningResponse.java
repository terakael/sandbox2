package main.responses;

import main.database.ItemDao;
import main.processing.Player;
import main.requests.Request;
import main.types.Items;

public class StartMiningResponse extends Response {

	public StartMiningResponse() {
		setAction("start_mining");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		ActionBubbleResponse actionBubble = new ActionBubbleResponse(player.getId(), ItemDao.getItem(Items.PICKAXE.getValue()).getSpriteFrameId());
		responseMaps.addLocalResponse(player.getRoomId(), player.getTileId(), actionBubble);
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
