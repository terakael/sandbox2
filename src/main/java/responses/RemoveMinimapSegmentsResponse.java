package responses;

import java.util.Set;

import lombok.Setter;
import processing.attackable.Player;
import requests.Request;

public class RemoveMinimapSegmentsResponse extends Response {
	@Setter private Set<Integer> segments;
	
	public RemoveMinimapSegmentsResponse() {
		setAction("remove_minimap_segments");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}

}
