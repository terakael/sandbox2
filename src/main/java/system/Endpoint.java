package system;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import processing.WorldProcessor;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import processing.managers.LocationManager;
import processing.managers.TradeManager;
import processing.managers.TradeManager.Trade;
import requests.CancelTradeRequest;
import requests.MultiRequest;
import requests.Request;
import requests.RequestDecoder;
import responses.ResponseEncoder;

@ServerEndpoint(value = "/game", encoders = ResponseEncoder.class, decoders = RequestDecoder.class)
public class Endpoint {
	// the reason we have two maps, is because in some cases we want the user to be able to do multiple requests in the same tick.
	// for example, equipping multiple items or banking multiple items within the same tick, otherwise they become annoying to do.
	// in addition, the multi-request requests can be done at the same time as another request, multi or not (walking plus equipping for example)
	private static Map<Session, Request> requestMap = new HashMap<>();
	private static Map<Session, List<Request>> multiRequestMap = new HashMap<>();
	
	public static Map<Session, List<Request>> takeRequests() {
		final Map<Session, List<Request>> requests = new HashMap<>();
		
		requestMap.forEach((session, request) -> {
			requests.putIfAbsent(session, new ArrayList<>());
			requests.get(session).add(request);
		});
		requestMap.clear();
		
		multiRequestMap.forEach((session, request) -> {
			requests.putIfAbsent(session, new ArrayList<>());
			requests.get(session).addAll(request);
		});
		multiRequestMap.clear();
		
		return requests;
	}
	
	@OnOpen
	public void onOpen(Session session) {
		System.out.println("onOpen");
	}
	
	@OnMessage
	public void onMessage(Request msg, Session client) {
		// collect the messages for the WorldProcessor to process every tick
		Player messagePlayer = WorldProcessor.playerSessions.get(client);
		if (messagePlayer != null)
			System.out.println(String.format("player %d (%s): %s", messagePlayer.getDto().getId(), messagePlayer.getDto().getName(), msg.getAction()));
			
		if (msg instanceof MultiRequest) {
			multiRequestMap.putIfAbsent(client, new ArrayList<>());
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
		
		final Player playerToRemove = WorldProcessor.playerSessions.get(session);
		if (playerToRemove == null)
			return;
		
//		FightManager.cancelFight(playerToRemove, null);
//		final Fight fight = FightManager.getFightWithFighter(playerToRemove);
//		if (fight != null) {
//			Attackable opponent = fight.getOtherFighter(playerToRemove);
//			if (opponent instanceof Player) {
//				requestMap.put(((Player)opponent).getSession(), new CancelFightRequest());
//			}
//		}
		
		final Trade trade = TradeManager.getTradeWithPlayer(playerToRemove);
		if (trade != null) {
			Player otherPlayer = trade.getOtherPlayer(playerToRemove);
			requestMap.put(otherPlayer.getSession(), new CancelTradeRequest());
		}
		
		if (playerToRemove.getPet() != null)
			LocationManager.removePetIfExists(playerToRemove.getPet());
		
		// if the player made any requests since the last tick before logout, remove them
		requestMap.remove(session);
		multiRequestMap.remove(session);
		
		WorldProcessor.playerSessions.remove(session);
		
		ClientResourceManager.decachePlayer(playerToRemove);
		
//		if (playerToRemove.getDto() != null) {
//			requestMap.put(session, new PlayerLeaveRequest(playerToRemove.getDto().getName()));
//		}
	}
}
