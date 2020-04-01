package main.responses;

import java.util.Set;

import lombok.Setter;
import main.processing.Player;
import main.requests.Request;

public class RemoveMinimapSegmentsResponse extends Response {
	@Setter private Set<Integer> segments;
	
	public RemoveMinimapSegmentsResponse() {
		setAction("remove_minimap_segments");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
