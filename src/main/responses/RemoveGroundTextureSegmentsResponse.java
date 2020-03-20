package main.responses;

import java.util.Map;
import java.util.Set;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class RemoveGroundTextureSegmentsResponse extends Response {
	@Setter private Map<Integer, Set<Integer>> segments; // <roomId, <segments>>
	
	public RemoveGroundTextureSegmentsResponse() {
		setAction("remove_ground_texture_segments");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		
	}

}
