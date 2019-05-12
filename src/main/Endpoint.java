package main;

import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import main.database.AnimationDao;
import main.database.DbConnection;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerSessionDao;
import main.processing.WorldProcessor;
import main.requests.Request;
import main.requests.RequestDecoder;
import main.responses.LogonResponse;
import main.responses.MessageResponse;
import main.responses.PlayerEnterResponse;
import main.responses.PlayerLeaveResponse;
import main.responses.Response;
import main.responses.ResponseFactory;
import main.state.Player;
import main.responses.Response.ResponseType;
import main.responses.ResponseEncoder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.*;

@ServerEndpoint(value = "/game", encoders = ResponseEncoder.class, decoders = RequestDecoder.class)
public class Endpoint {
	private static Gson gson = new Gson();
	
	// connections to the server; not necessarily players (i.e. logon screen)
	private static Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());
	
	// players actively ingame
	public static Map<PlayerDto, Session> playerSessions = new HashMap<>();// TODO move to WorldProcessor
	
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
	
	public static void broadcastMessageToEveryone(String text, String colour) {
		MessageResponse msgResp = new MessageResponse("message");
		msgResp.setRecoAndResponseText(1, "");
		msgResp.setColour(colour);
		msgResp.setMessage(text);
		sendTextToEveryone(gson.toJson(msgResp), null, true);
	}
	
	public static void sendTextToEveryone(String text, Session client, boolean includeClient) {
		for (Map.Entry<PlayerDto, Session> peer : playerSessions.entrySet()) {
			if (!includeClient && peer.getValue() == client)
				continue;
			try {
				peer.getValue().getBasicRemote().sendText(text);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@OnClose
	public void onClose(Session session) {
		// TODO: all good to clear the session in normal cases, but in combat they should stay 
		// in-game for like a minute to prevent x-logging when in danger
		System.out.println("onClose");
		peers.remove(session);
		
		PlayerDto key = null;
		for (Map.Entry<PlayerDto, Session> peer : playerSessions.entrySet()) {
			if (peer.getValue() == session) {
				key = peer.getKey();
				break;
			}
		}
		
		WorldProcessor.playerSessions.remove(session);
		
		if (key != null) {
			playerSessions.remove(key);
			PlayerSessionDao.removePlayer(key.getId());
			
			PlayerLeaveResponse leave = new PlayerLeaveResponse("playerLeave");
			leave.setId(key.getId());
			leave.setName(key.getName());
			sendTextToEveryone(gson.toJson(leave), null, false);
		}
	}
	
	public static Session getSessionByPlayerId(int id) {		
		for (Map.Entry<PlayerDto, Session> entry : Endpoint.playerSessions.entrySet()) {
			if (entry.getKey().getId() == id)
				return entry.getValue();
		}
		return null;
	}

}
