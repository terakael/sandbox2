package responses;

import java.util.Collections;

import database.dto.ItemDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;

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
