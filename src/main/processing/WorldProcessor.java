package main.processing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.Session;

import com.google.gson.Gson;

import main.Endpoint;
import main.FightManager;
import main.requests.Request;
import main.responses.Response;
import main.responses.ResponseFactory;
import main.responses.ResponseMaps;
import main.state.Player;

public class WorldProcessor implements Runnable {
	private Thread thread;
	private static final int TICK_DURATION = 600;
	private static Gson gson = new Gson();
	
//	private Map<Player, ArrayList<Response>> clientOnlyResponses = new HashMap<>();
//	private Map<Player, ArrayList<Response>> localResponses = new HashMap<>();
//	private ArrayList<Response> broadcastResponses = new ArrayList<>();
//	private Map<Player, ArrayList<Response>> broadcastExcludeClientResponses = new HashMap<>();
	public static Map<Session, Player> playerSessions = new HashMap<>();
	
	public void start() {
		if (thread == null) {
			thread = new Thread(this, "worldprocessor");
			thread.start();
		}
	}
	
	@Override
	public void run() {
		while (true) {
			long prevTime = System.nanoTime();
			
			process();
			
			try {
				// run for 0.6 seconds, minus the processing time so the ticks are always 0.6s.
				// if the processing time is more than 0.6 seconds then don't sleep at all (this happens when breakpoints are hit)
				Thread.sleep(Math.max(0, TICK_DURATION - ((System.nanoTime() - prevTime) / 1000000)));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void process() {		
		// pull requestmap contents from Endpoint and clear it so it can collect for the next tick
		Map<Session, Request> requestMap = new HashMap<>();
		requestMap.putAll(Endpoint.requestMap);
		Endpoint.requestMap.clear();
		
		// process all requests and add all responses to this object which will be compiled into the response list for each player
		ResponseMaps responseMaps = new ResponseMaps();
		
		// process player requests for this tick
		for (Map.Entry<Session, Request> entry : requestMap.entrySet()) {
			final Request request = entry.getValue();
			Response response = ResponseFactory.create(request.getAction());
			response.process(request, entry.getKey(), responseMaps);
		}
		
		// process players
		for (Map.Entry<Session, Player> entry : playerSessions.entrySet()) {
			entry.getValue().process(responseMaps);
		}
		
		// process fight manager
		FightManager.process(responseMaps);
		
		// take all the responseMaps and compile the responses to send to each player
		HashMap<Player, ArrayList<Response>> clientResponses = new HashMap<>();
		compileBroadcastResponses(clientResponses, responseMaps);
		compileBroadcastExcludeResponses(clientResponses, responseMaps);
		compileLocalResponses(clientResponses, responseMaps);
		compileClientOnlyResponses(clientResponses, responseMaps);
		
		// process npcs
		// TODO
		
		// go through the clientResponses and send the response array to each player
		for (Map.Entry<Player, ArrayList<Response>> responses : clientResponses.entrySet()) {
			try {
				responses.getKey().getSession().getBasicRemote().sendText(gson.toJson(responses.getValue()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void compileBroadcastResponses(HashMap<Player, ArrayList<Response>> clientResponses, ResponseMaps responseMaps) {
		for (Map.Entry<Session, Player> playerSession : playerSessions.entrySet()) {
			// every player gets the broadcast responses
			for (Response broadcastResponse : responseMaps.getBroadcastResponses()) {
				if (!clientResponses.containsKey(playerSession.getValue()))
					clientResponses.put(playerSession.getValue(), new ArrayList<>());
				clientResponses.get(playerSession.getValue()).add(broadcastResponse);
			}
		}
	}
	
	private void compileBroadcastExcludeResponses(HashMap<Player, ArrayList<Response>> clientResponses, ResponseMaps responseMaps) {
		for (Map.Entry<Player, ArrayList<Response>> broadcastResponseMap : responseMaps.getBroadcastExcludeClientResponses().entrySet()) {
			for (Map.Entry<Session, Player> playerSession : playerSessions.entrySet()) {
				if (playerSession.getValue().equals(broadcastResponseMap.getKey()))
					continue;// don't send to client
				
				if (!clientResponses.containsKey(playerSession.getValue()))
					clientResponses.put(playerSession.getValue(), new ArrayList<>());
				
				for (Response response : broadcastResponseMap.getValue())
					clientResponses.get(playerSession.getValue()).add(response);
			}
		}
	}
	
	private void compileLocalResponses(HashMap<Player, ArrayList<Response>> clientResponses, ResponseMaps responseMaps) {
		for (Map.Entry<Player, ArrayList<Response>> localResponseMap : responseMaps.getLocalResponses().entrySet()) {
			ArrayList<Player> localPlayers = getPlayersNearTile(localResponseMap.getKey().getTileId(), 15);
			for (Player localPlayer : localPlayers) {
				if (!clientResponses.containsKey(localPlayer))
					clientResponses.put(localPlayer, new ArrayList<>());
				
				for (Response response : localResponseMap.getValue())
					clientResponses.get(localPlayer).add(response);
			}
		}
	}

	private void compileClientOnlyResponses(HashMap<Player, ArrayList<Response>> clientResponses, ResponseMaps responseMaps) {
		for (Map.Entry<Player, ArrayList<Response>> privateResponseMap : responseMaps.getClientOnlyResponses().entrySet()) {
			// only individual players get these responses
			for (Response privateResponse : privateResponseMap.getValue()) {
				if (!clientResponses.containsKey(privateResponseMap.getKey()))
					clientResponses.put(privateResponseMap.getKey(), new ArrayList<>());
				clientResponses.get(privateResponseMap.getKey()).add(privateResponse);
			}
		}
	}
	
	private ArrayList<Player> getPlayersNearTile(int tileId, int radius) {
		ArrayList<Player> localPlayers = new ArrayList<>();
		
		int tileX = tileId % PathFinder.LENGTH;
		int tileY = tileId / PathFinder.LENGTH;
		for (Player player : WorldProcessor.playerSessions.values()) {
			int testTileX = player.getTileId() % PathFinder.LENGTH;
			int testTileY = player.getTileId() / PathFinder.LENGTH;
			
			if ((testTileX >= tileX - radius && testTileX <= tileX + radius) &&
				(testTileY >= tileY - radius && testTileY <= tileY + radius)) {
				localPlayers.add(player);
			}
		}
		
		return localPlayers;
	}
}
