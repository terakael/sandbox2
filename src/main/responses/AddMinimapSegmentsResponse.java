package main.responses;

import java.util.Map;
import java.util.Set;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class AddMinimapSegmentsResponse extends Response {
	@Setter Map<Integer, String> segments;
	@Setter Map<Integer, Set<Integer>> minimapIconLocations; // spriteFrameId, tileIds
	
	public AddMinimapSegmentsResponse() {
		setAction("add_minimap_segments");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
