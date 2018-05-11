package main;

import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import main.database.DbConnection;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerSessionDao;
import main.requests.Request;
import main.requests.RequestDecoder;
import main.responses.LogonResponse;
import main.responses.MessageResponse;
import main.responses.PlayerEnterResponse;
import main.responses.Response;
import main.responses.ResponseFactory;
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
	private static Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());
	
	// TODO: this is a bit weird but its for the logoff message; ties a session to a player name
	private static Map<Session, PlayerDto> playerSessions = new HashMap<>();
	
	static {
		PlayerSessionDao.setDb(DbConnection.get());
		PlayerSessionDao.clearAllSessions();
	}
	
	@OnOpen
	public void onOpen(Session session) {
		System.out.println("onOpen");
		peers.add(session);
	}
	
	@OnMessage
	public void onMessage(Request msg, Session client) {
		System.out.println("req: " + msg.getAction());
		Response response = ResponseFactory.create(msg.getAction());
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			boolean sendToEveryone = response.process(msg);
			String respAsc = gson.toJson(response);
			
			// eventually this would be range-based, based on the player position.
			// probably the bool return would turn into an enum:
			// broadcastToEveryone, broadcastLocalToClient, sendToClient
			if (sendToEveryone) {
				sendTextToEveryone(respAsc, client, true);
			} else {
				if (msg.getAction().equals("logon") && response.getSuccess() == 1) {					
					// let everyone know this cool guy has logged in					
					LogonResponse logonResp = (LogonResponse)response;
					int userId = Integer.parseInt(logonResp.getUserId());
					
					PlayerDto player = new PlayerDto(userId, logonResp.getUsername(), "", logonResp.getX(), logonResp.getY());
					PlayerSessionDao.addPlayer(userId);
					playerSessions.put(client, player);
					peers.remove(client);
					
					PlayerEnterResponse enter = new PlayerEnterResponse("playerEnter");
					enter.setPlayer(player);
					sendTextToEveryone(gson.toJson(enter), client, false);// dont send playerEnter to player logging in
				}
				client.getBasicRemote().sendText(respAsc);
			}
		} catch (JsonParseException e) {
			response.setRecoAndResponseText(0, "json parse exception");
		} catch (IOException e) {
			response.setRecoAndResponseText(0, "io exception");
		}
	}
	
	private void broadcastMessageToEveryone(String text, String colour) {
		MessageResponse msgResp = new MessageResponse("message");
		msgResp.setRecoAndResponseText(1, "");
		msgResp.setColour(colour);
		msgResp.setMessage(text);
		sendTextToEveryone(gson.toJson(msgResp), null, true);
	}
	
	private void sendTextToEveryone(String text, Session client, boolean includeClient) {
		for (Map.Entry<Session, PlayerDto> peer : playerSessions.entrySet()) {
			if (!includeClient && peer.getKey() == client)
				continue;
			try {
				peer.getKey().getBasicRemote().sendText(text);
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
		
		if (playerSessions.containsKey(session)) {
			PlayerDto player = playerSessions.get(session);
			playerSessions.remove(session);
			PlayerSessionDao.removePlayer(player.getId());
			broadcastMessageToEveryone(String.format("%s has logged out.", player.getName()), "#0ff");
		}
	}

}
