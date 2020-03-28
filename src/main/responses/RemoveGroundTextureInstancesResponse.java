package main.responses;

import java.util.Set;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class RemoveGroundTextureInstancesResponse extends Response {
	@Setter private Set<Integer> tileIds;
	
	public RemoveGroundTextureInstancesResponse() {
		setAction("remove_ground_texture_instances");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		
	}

}
