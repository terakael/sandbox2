package main.responses;

import main.database.ItemDao;
import main.database.ItemDto;
import main.processing.Player;
import main.requests.Request;
import main.requests.SmithRequest;

public class StartSmithResponse extends Response {
	private int iconId;
	
	public StartSmithResponse() {
		setAction("start_smith");
	}
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		setRecoAndResponseText(1, "you place the ore in the furnace...");
		
		ItemDto dto = ItemDao.getItem(((SmithRequest)req).getItemId());
		if (dto == null)
			return;
		
		iconId = dto.getSpriteFrameId();
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
