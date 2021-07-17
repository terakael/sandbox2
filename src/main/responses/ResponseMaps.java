package main.responses;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import main.processing.attackable.Player;

@Getter
public class ResponseMaps {
	private Map<Player, List<Response>> clientOnlyResponses = new LinkedHashMap<>();// we use linked has maps to keep the insertion order, this is important
	private Map<Integer, Map<Integer, List<Response>>> localResponses = new LinkedHashMap<>();// key is the central tileId
	private List<Response> broadcastResponses = new ArrayList<>();
	private Map<Player, List<Response>> broadcastExcludeClientResponses = new LinkedHashMap<>();
	
	public void addClientOnlyResponse(Player player, Response response) {
		if (!clientOnlyResponses.containsKey(player))
			clientOnlyResponses.put(player, new ArrayList<>());
		clientOnlyResponses.get(player).add(response);
	}
	
	public void addLocalResponse(Integer floor, Integer tileId, Response response) {
		if (!localResponses.containsKey(floor))
			localResponses.put(floor, new LinkedHashMap<>());
		
		if (!localResponses.get(floor).containsKey(tileId))
			localResponses.get(floor).put(tileId, new ArrayList<>());
		localResponses.get(floor).get(tileId).add(response);
	}
	
	public void addBroadcastResponse(Response response, Player playerToExclude) {
		if (playerToExclude == null) {
			broadcastResponses.add(response);
		} else {
			if (!broadcastExcludeClientResponses.containsKey(playerToExclude))
				broadcastExcludeClientResponses.put(playerToExclude, new ArrayList<>());
			broadcastExcludeClientResponses.get(playerToExclude).add(response);
		}
	}
	
	public void addBroadcastResponse(Response response) {
		addBroadcastResponse(response, null);
	}
}
