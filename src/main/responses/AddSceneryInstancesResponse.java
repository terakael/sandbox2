package main.responses;

import java.util.Map;
import java.util.Set;
import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class AddSceneryInstancesResponse extends Response {
	@Setter private Map<Integer, Set<Integer>> instances; // <sceneryId, <tileIds>>>
	@Setter private Set<Integer> depletedScenery = null;
	
	public AddSceneryInstancesResponse() {
		setAction("add_scenery_instances");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
	}	
}
