package main.responses;

import java.util.List;
import java.util.Map;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class AddGroundTextureSegmentsResponse extends Response {
	@Setter private Map<Integer, Map<Integer, List<Integer>>> segments; // <roomId, <segmentId, <groundTextureId>>>
	
	public AddGroundTextureSegmentsResponse() {
		setAction("add_ground_texture_segments");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
