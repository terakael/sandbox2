package main.responses;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class GroundItemRefreshResponse extends Response {
	public GroundItemRefreshResponse() {
		setAction("ground_item_refresh");
	}
	@Setter private HashMap<Integer, ArrayList<Integer>> groundItems = new HashMap<>();

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
	public void addGroundItem(int tileId, int itemId) {
		if (!groundItems.containsKey(tileId))
			groundItems.put(tileId, new ArrayList<>());
		groundItems.get(tileId).add(itemId);
	}

}
