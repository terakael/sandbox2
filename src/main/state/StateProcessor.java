package main.state;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import javax.websocket.Session;

import com.google.gson.Gson;

import main.Endpoint;
import main.database.PlayerDto;
import main.database.PlayerSessionDao;
import main.requests.Request;
import main.responses.MessageResponse;
import main.responses.PlayerLeaveResponse;
import main.responses.Response;
import main.responses.ResponseFactory;
import main.responses.Response.ResponseType;

public class StateProcessor implements Runnable {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
//	private Thread thread;
//	private final float TICK_RATE_MS = 0.5f;
//	private float timer = TICK_RATE_MS;
//	
//	// connections to the server; not necessarily players (i.e. logon screen)
//	private static Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());
//	
//	// in-memory players to process
//	private static Map<Integer, Player> players = new HashMap<>();
//
//	private static Gson gson = new Gson();
//	private ConcurrentLinkedQueue<Response> broadcastResponses = new ConcurrentLinkedQueue<>();
//	private ConcurrentMap<Session, ConcurrentLinkedQueue<Response>> clientResponses = new ConcurrentHashMap<>();
//	private Map<Session, MoveAndActionRequest> moveAndActionRequests = new HashMap<>();
//	
//	private ConcurrentMap<Session, Request> requestMap = new ConcurrentHashMap<>();
//	
//	private static StateProcessor singleton;
//	static {
//		singleton = new StateProcessor();
//	}
//	
//	private StateProcessor() {};
//	
//	static int counter = 0;
//	public void process(float dt) {
//		timer -= dt;
//		if (timer <= 0) {
//			timer = TICK_RATE_MS;
//			processGameState(TICK_RATE_MS);
//			processMoveAndActionRequests(TICK_RATE_MS);
//			processRequestMap();
//			broadcastQueuedResponses();
//		}
//	}
//	
//	private void processGameState(float dt) {
//		for (Player p : players.values()) {
//			p.process(dt);
//		}
//	}
//	
//	private void processRequestMap() {
//		for (Map.Entry<Session, Request> entry : requestMap.entrySet()) {
//			Request request = entry.getValue();
//			Session session = entry.getKey();
//
//			Response response = request.process(session);
//			switch (response.getResponseType()) {
//			case broadcast:
//				addForBroadcast(response);
//				break;
//			case client_only:
//				addForClient(session, response);
//				break;
//			default:
//				break;
//			}
//		}
//		
//		requestMap.clear();
//	}
//	
//	public void broadcastQueuedResponses() {
//		// broadcasts periodically to all players (eventually all players local to area)
//		// combine all the broadcastResponses into a single response
//		// then as we run through the list of sessions we add the clientResponses when needed
//		
//		List<Response> forEveryone = new ArrayList<>();
//		while (!broadcastResponses.isEmpty()) {
//			forEveryone.add(broadcastResponses.remove());
//		}
//		
//		for (Player p : players.values()) {
//			try {
//				List<Response> forClient = new ArrayList<>();
//				if (clientResponses.containsKey(p.getClient())) {
//					ConcurrentLinkedQueue<Response> clientResponseMap = clientResponses.get(p.getClient());
//					while (!clientResponseMap.isEmpty())
//						forClient.add(clientResponseMap.remove());
//				}
//				forClient.addAll(forEveryone);
//
//				if (forClient.size() > 0)
//					p.getClient().getBasicRemote().sendText(gson.toJson(forClient));
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//
//	@Override
//	public void run() {
//		long prevTime = System.currentTimeMillis();
//		while (true) {
//			long time = System.currentTimeMillis();
//			process((float)(time - prevTime) / 1000.0f);
//			prevTime = time;
//		}
//	}
//	
//	public void start() {
//		if (thread == null) {
//			thread = new Thread(this, "stateprocessor");
//			thread.start();
//		}
//	}
//	
//	public static StateProcessor get() {
//		return singleton;
//	}
//
//	public void addForBroadcast(Response response) {
//		broadcastResponses.add(response);
//	}
//
//	public void addForClient(Session client, Response response) {
//		if (!clientResponses.containsKey(client))
//			clientResponses.put(client, new ConcurrentLinkedQueue<>());
//		clientResponses.get(client).add(response);
//	}
//
//	public void addForAllExceptClient(Response response, Session client) {
//		for (Player p : players.values()) {
//			if (p.getClient() != client)
//				addForClient(p.getClient(), response);
//		}
//	}
//	
//	public void addRequest(Session client, Request request) {
//		requestMap.put(client, request);
//	}
//
//	public void addSession(Session session) {
//		peers.add(session);
//	}
//	
//	public static void broadcastMessageToEveryone(String text, String colour) {
//		MessageResponse msgResp = new MessageResponse("message");
//		msgResp.setRecoAndResponseText(1, "");
//		msgResp.setColour(colour);
//		msgResp.setMessage(text);
//		sendTextToEveryone(gson.toJson(msgResp), null, true);
//	}
//	
//	public static void sendTextToEveryone(String text, Session client, boolean includeClient) {
//		for (Player p : players.values()) {
//			if (!includeClient && p.getClient() == client)
//				continue;
//			try {
//				p.getClient().getBasicRemote().sendText(text);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
//
//	public void removePeer(Session session) {
//		peers.remove(session);
//	}
//
//	public void logoutPlayerBySession(Session session) {
//		Player logoutPlayer = null;
//		for (Player p : players.values()) {
//			if (p.getClient() == session) {
//				logoutPlayer = p;
//				break;
//			}
//		}
//		
//		if (logoutPlayer != null) {
//			players.remove(logoutPlayer.getId());
//			PlayerSessionDao.removePlayer(logoutPlayer.getId());
//			
//			PlayerLeaveResponse leave = new PlayerLeaveResponse("playerLeave");
//			leave.setId(logoutPlayer.getId());
//			leave.setName(logoutPlayer.getName());
//			addForBroadcast(leave);
//		}
//	}
//	
//	private void processMoveAndActionRequests(float dt) {
//		Map<Session, MoveAndActionRequest> finishedRequests = new HashMap<>();
//		for (Map.Entry<Session, MoveAndActionRequest> entry : moveAndActionRequests.entrySet()) {
//			if (entry.getValue().process(dt)) {
//				addRequest(entry.getKey(), entry.getValue().getRequest());
//				finishedRequests.put(entry.getKey(), entry.getValue());
//			}
//		}
//		
//		for (Session client : finishedRequests.keySet()) {
//			moveAndActionRequests.remove(client);
//		}
//	}
//	
//	public static Session getSessionByPlayerId(int id) {		
//		if (players.containsKey(id))
//			return players.get(id).getClient();
//		return null;
//	}
//	
//	public static void addPlayerSession(PlayerDto player, Session client) {
//		players.put(player.getId(), new Player(player, client));
//		peers.remove(client);
//	}
//
//	public void addMoveAndActionRequest(Session client, MoveAndActionRequest moveAndActionRequest) {
//		moveAndActionRequests.put(client, moveAndActionRequest);
//	}
//
//	public Player getPlayer(int id) {
//		return players.get(id);
//	}
	
}
