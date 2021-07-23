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
import processing.managers.FightManager;
import processing.managers.TradeManager;
import processing.managers.TradeManager.Trade;
import requests.CancelTradeRequest;
import requests.MultiRequest;
import requests.PlayerLeaveRequest;
import requests.Request;
import requests.RequestDecoder;
import responses.ResponseEncoder;

@SuppressWarnings("unused")
@ServerEndpoint(value = "/game", encoders = ResponseEncoder.class, decoders = RequestDecoder.class)
public class Endpoint {
	// the reason we have two maps, is because in some cases we want the user to be able to do multiple requests in the same tick.
	// for example, equipping multiple items or banking multiple items within the same tick, otherwise they become annoying to do.
	// in addition, the multi-request requests can be done at the same time as another request, multi or not (walking plus equipping for example)
	public static Map<Session, Request> requestMap = new HashMap<>();
	public static Map<Session, List<Request>> multiRequestMap = new HashMap<>();
	
	@OnOpen
	public void onOpen(Session session) {
		System.out.println("onOpen");
	}
	
	@OnMessage
	public void onMessage(Request msg, Session client) {
		// collect the messages for the WorldProcessor to process every tick
		System.out.println("action: " + msg.getAction());
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
		
		Player playerToRemove = WorldProcessor.playerSessions.get(session);
		if (playerToRemove == null)
			return;
		
		FightManager.cancelFight(playerToRemove, null);
		Trade trade = TradeManager.getTradeWithPlayer(playerToRemove);
		if (trade != null) {
			Player otherPlayer = trade.getOtherPlayer(playerToRemove);
			requestMap.put(otherPlayer.getSession(), new CancelTradeRequest());
		}
		
		ClientResourceManager.decachePlayer(playerToRemove);
		
		WorldProcessor.playerSessions.remove(session);
		
		if (playerToRemove.getDto() != null) {
			requestMap.put(session, new PlayerLeaveRequest(playerToRemove.getDto().getName()));
		}
	}
}
