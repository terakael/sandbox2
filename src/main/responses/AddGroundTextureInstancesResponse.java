package main.responses;

import java.util.Map;
import java.util.Set;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class AddGroundTextureInstancesResponse extends Response {
	@Setter private Map<Integer, Set<Integer>> instances; // groundTextureId, <tileIds>
	
	public AddGroundTextureInstancesResponse() {
		setAction("add_ground_texture_instances");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
