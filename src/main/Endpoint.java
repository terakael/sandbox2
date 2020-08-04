package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;

import main.processing.FightManager;
import main.processing.Player;
import main.processing.TradeManager;
import main.processing.WorldProcessor;
import main.requests.MultiRequest;
import main.requests.PlayerLeaveRequest;
import main.requests.Request;
import main.requests.RequestDecoder;
import main.responses.ResponseEncoder;

@ServerEndpoint(value = "/game", encoders = ResponseEncoder.class, decoders = RequestDecoder.class)
public class Endpoint {
	private static Gson gson = new Gson();
	
	// connections to the server; not necessarily players (i.e. logon screen)
	private static Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());
	
	// the reason we have two maps, is because in some cases we want the user to be able to do multiple requests in the same tick.
	// for example, equipping multiple items or banking multiple items within the same tick, otherwise they become annoying to do.
	// in addition, the multi-request requests can be done at the same time as another request, multi or not (walking plus equipping for example)
	public static Map<Session, Request> requestMap = new HashMap<>();
	public static Map<Session, List<Request>> multiRequestMap = new HashMap<>();
	
//	static {
//		PlayerSessionDao.clearAllSessions();
//	}
	
	@OnOpen
	public void onOpen(Session session) {
		System.out.println("onOpen");
	}
	
	@OnMessage
	public void onMessage(Request msg, Session client) {
		// collect the messages for the WorldProcessor to process every tick
		System.out.println("req: id: " + msg.getId() +  " action: " + msg.getAction());
		if (msg instanceof MultiRequest) {
			if (!multiRequestMap.containsKey(client))
				multiRequestMap.put(client, new ArrayList<>());
			multiRequestMap.get(client).add(msg);
		} else {
			requestMap.put(client, msg);
		}
	}
	
	@OnClose
	public void onClose(Session session) {
		// TODO: all good to clear the session in normal cases, but in combat they should stay 
		// in-game for like a minute to prevent x-logging when in danger
		System.out.println("onClose");
//		peers.remove(session);
		
		Player playerToRemove = WorldProcessor.playerSessions.get(session);
		if (playerToRemove == null)
			return;
		
		FightManager.cancelFight(playerToRemove, null);
		TradeManager.cancelTrade(playerToRemove);
		
		WorldProcessor.playerSessions.remove(session);
		
		if (playerToRemove.getDto() != null) {
			PlayerLeaveRequest req = new PlayerLeaveRequest();
			req.setAction("playerLeave");
			req.setId(playerToRemove.getId());
			req.setName(playerToRemove.getDto().getName());
			requestMap.put(session, req);
		}
	}
}
