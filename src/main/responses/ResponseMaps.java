package main.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import main.processing.Player;

@Getter
public class ResponseMaps {
	private Map<Player, ArrayList<Response>> clientOnlyResponses = new HashMap<>();
	private Map<Player, ArrayList<Response>> localResponses = new HashMap<>();
	private ArrayList<Response> broadcastResponses = new ArrayList<>();
	private Map<Player, ArrayList<Response>> broadcastExcludeClientResponses = new HashMap<>();
	
	public void addClientOnlyResponse(Player player, Response response) {
		if (!clientOnlyResponses.containsKey(player))
			clientOnlyResponses.put(player, new ArrayList<>());
		clientOnlyResponses.get(player).add(response);
	}
	
	public void addLocalResponse(Player player, Response response) {
		if (!localResponses.containsKey(player))
			localResponses.put(player, new ArrayList<>());
		localResponses.get(player).add(response);
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
