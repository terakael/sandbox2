package main;

import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;
import main.database.PlayerSessionDao;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.requests.PlayerLeaveRequest;
import main.requests.Request;
import main.requests.RequestDecoder;
import main.responses.ResponseEncoder;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.websocket.*;

@ServerEndpoint(value = "/game", encoders = ResponseEncoder.class, decoders = RequestDecoder.class)
public class Endpoint {
	private static Gson gson = new Gson();
	
	// connections to the server; not necessarily players (i.e. logon screen)
	private static Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());
	
	public static Map<Session, Request> requestMap = new HashMap<>();
	
	static {
		PlayerSessionDao.clearAllSessions();
	}
	
	@OnOpen
	public void onOpen(Session session) {
		System.out.println("onOpen");
		peers.add(session);
	}
	
	@OnMessage
	public void onMessage(Request msg, Session client) {
		// collect the messages for the WorldProcessor to process every tick
		System.out.println("req: id: " + msg.getId() +  " action: " + msg.getAction());
		requestMap.put(client, msg);
	}
	
	@OnClose
	public void onClose(Session session) {
		// TODO: all good to clear the session in normal cases, but in combat they should stay 
		// in-game for like a minute to prevent x-logging when in danger
		System.out.println("onClose");
		peers.remove(session);
		
		Player playerToRemove = WorldProcessor.playerSessions.get(session);
		if (playerToRemove == null)
			return;
		
		FightManager.cancelFight(playerToRemove.getId());
		
		WorldProcessor.playerSessions.remove(session);
		
		if (playerToRemove.getDto() != null) {
			PlayerSessionDao.removePlayer(playerToRemove.getDto().getId());
			
			PlayerLeaveRequest req = new PlayerLeaveRequest();
			req.setAction("playerLeave");
			req.setId(playerToRemove.getDto().getId());
			req.setName(playerToRemove.getDto().getName());
			requestMap.put(session, req);
		}
	}
}
