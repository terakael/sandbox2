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
import main.responses.LogonResponse;
import main.responses.PlayerEnterResponse;
import main.responses.Response;
import main.responses.ResponseFactory;
import main.responses.ResponseMaps;
import main.responses.Response.ResponseType;
import main.state.Player;

public class WorldProcessor implements Runnable {
	private Thread thread;
	private static final int TICK_DURATION = 600;
	
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
		// process request map
		Map<Session, Request> requestMap = new HashMap<>();
		requestMap.putAll(Endpoint.requestMap);
		Endpoint.requestMap.clear();
		
		ResponseMaps responseMaps = new ResponseMaps();
		
		for (Map.Entry<Session, Request> entry : requestMap.entrySet()) {
			Request request = entry.getValue();
			Response response = ResponseFactory.create(request.getAction());
			ResponseType responseType = response.process(request, entry.getKey(), responseMaps);
			
			Player player = playerSessions.get(entry.getKey());
			
			if (response instanceof LogonResponse) {
				PlayerEnterResponse playerEnter = new PlayerEnterResponse("playerEnter");
				playerEnter.setPlayer(player.getDto());
				responseMaps.addBroadcastResponse(playerEnter, player);
			}
		}
		
		// process players
		for (Map.Entry<Session, Player> entry : playerSessions.entrySet()) {
			entry.getValue().process(responseMaps);
		}
		
		// process fight manager
		FightManager.process(responseMaps);
		
		HashMap<Player, ArrayList<Response>> clientResponses = new HashMap<>();
		for (Map.Entry<Session, Player> playerSession : playerSessions.entrySet()) {
			// every player gets the broadcast responses
			for (Response broadcastResponse : responseMaps.getBroadcastResponses()) {
				if (!clientResponses.containsKey(playerSession.getValue()))
					clientResponses.put(playerSession.getValue(), new ArrayList<>());
				clientResponses.get(playerSession.getValue()).add(broadcastResponse);
			}
		}
		
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
		
		
		for (Map.Entry<Player, ArrayList<Response>> localResponseMap : responseMaps.getLocalResponses().entrySet()) {
			// TODO should be local players instead of all players
			for (Map.Entry<Session, Player> playerSession : playerSessions.entrySet()) {
				if (!clientResponses.containsKey(playerSession.getValue()))
					clientResponses.put(playerSession.getValue(), new ArrayList<>());
				
				for (Response response : localResponseMap.getValue())
					clientResponses.get(playerSession.getValue()).add(response);
			}
		}
		
		for (Map.Entry<Player, ArrayList<Response>> privateResponseMap : responseMaps.getClientOnlyResponses().entrySet()) {
			// only individual players get these responses
			for (Response privateResponse : privateResponseMap.getValue()) {
				if (!clientResponses.containsKey(privateResponseMap.getKey()))
					clientResponses.put(privateResponseMap.getKey(), new ArrayList<>());
				clientResponses.get(privateResponseMap.getKey()).add(privateResponse);
			}
		}
		
		// process npcs
		// process fight manager
		
		// prepare response maps
		// send responses to everyone
		//String respAsc = gson.toJson(response);
		Gson gson = new Gson();
		for (Map.Entry<Player, ArrayList<Response>> responses : clientResponses.entrySet()) {
			String respAsc = gson.toJson(responses.getValue());
			try {
				responses.getKey().getSession().getBasicRemote().sendText(respAsc);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
