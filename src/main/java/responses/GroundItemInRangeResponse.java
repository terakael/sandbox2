package responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

public class GroundItemInRangeResponse extends Response {
	@Setter private Map<Integer, List<Integer>> groundItems = new HashMap<>();
	
	public GroundItemInRangeResponse() {
		setAction("ground_item_in_range");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		
	}

}
