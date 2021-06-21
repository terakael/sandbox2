package main.responses;

import java.util.Map;
import java.util.Set;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class ConstructableInRangeResponse extends Response {
	@Setter private Map<Integer, Set<Integer>> instances; // <sceneryId, <tileIds>>>
	
	public ConstructableInRangeResponse() {
		setAction("constructable_in_range");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		
	}
	
}
