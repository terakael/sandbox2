package responses;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

public class GroundItemRefreshResponse extends Response {
	public GroundItemRefreshResponse() {
		setAction("ground_item_refresh");
	}
	@Setter private HashMap<Integer, ArrayList<Integer>> groundItems = new HashMap<>();

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}
	
	public void addGroundItem(int tileId, int itemId) {
		if (!groundItems.containsKey(tileId))
			groundItems.put(tileId, new ArrayList<>());
		groundItems.get(tileId).add(itemId);
	}

}
