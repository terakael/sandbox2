package responses;

import java.util.Collections;

import database.dto.ItemDto;
import processing.WorldProcessor;
import processing.attackable.Player;
import processing.attackable.Ship;
import processing.managers.ClientResourceManager;
import requests.Request;

@SuppressWarnings("unused")
public class ActionBubbleResponse extends Response {
	private Integer playerId = null;
	private Integer shipId = null;
	private int iconId;
	
	public ActionBubbleResponse(Player player, ItemDto item) {
		setAction("action_bubble");
		this.playerId = player.getId();
		this.iconId = item.getSpriteFrameId();
		
		ClientResourceManager.addLocalItems(player, Collections.singleton(item.getId()));
	}
	
	public ActionBubbleResponse(Ship ship, int playerId, ItemDto item) {
		setAction("action_bubble");
		this.shipId = ship.getCaptainId();
		this.playerId = playerId;
		this.iconId = item.getSpriteFrameId();
		
		ClientResourceManager.addLocalItems(WorldProcessor.getPlayerById(ship.getCaptainId()), Collections.singleton(item.getId()));
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
}
