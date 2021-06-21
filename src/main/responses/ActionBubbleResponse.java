package main.responses;

import java.util.Collections;

import main.database.dto.ItemDto;
import main.processing.ClientResourceManager;
import main.processing.Player;
import main.requests.Request;

@SuppressWarnings("unused")
public class ActionBubbleResponse extends Response {
	private int playerId;
	private int iconId;
	
	public ActionBubbleResponse(Player player, ItemDto item) {
		setAction("action_bubble");
		this.playerId = player.getId();
		this.iconId = item.getSpriteFrameId();
		
		ClientResourceManager.addLocalItems(player, Collections.singleton(item.getId()));
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
}
