package main.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import main.processing.Player;

@Getter
public class ResponseMaps {
	private Map<Player, ArrayList<Response>> clientOnlyResponses = new HashMap<>();
	private Map<Integer, Map<Integer, ArrayList<Response>>> localResponses = new HashMap<>();// key is the central tileId
	private ArrayList<Response> broadcastResponses = new ArrayList<>();
	private Map<Player, ArrayList<Response>> broadcastExcludeClientResponses = new HashMap<>();
	
	public void addClientOnlyResponse(Player player, Response response) {
		if (!clientOnlyResponses.containsKey(player))
			clientOnlyResponses.put(player, new ArrayList<>());
		clientOnlyResponses.get(player).add(response);
	}
	
	public void addLocalResponse(Integer roomId, Integer tileId, Response response) {
		if (!localResponses.containsKey(roomId))
			localResponses.put(roomId, new HashMap<>());
		
		if (!localResponses.get(roomId).containsKey(tileId))
			localResponses.get(roomId).put(tileId, new ArrayList<>());
		localResponses.get(roomId).get(tileId).add(response);
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
