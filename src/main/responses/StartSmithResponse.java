package main.responses;

import main.database.ItemDao;
import main.database.ItemDto;
import main.processing.Player;
import main.requests.Request;
import main.requests.SmithRequest;

public class StartSmithResponse extends Response {
	public StartSmithResponse() {
		setAction("start_smith");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		setRecoAndResponseText(1, "you place the ore in the furnace...");
		
		ItemDto dto = ItemDao.getItem(((SmithRequest)req).getItemId());
		if (dto == null)
			return;
		
		ActionBubbleResponse actionBubble = new ActionBubbleResponse(player.getId(), dto.getSpriteFrameId());
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), actionBubble);
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
