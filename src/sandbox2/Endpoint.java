package sandbox2;

import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import sandbox2.requests.Request;
import sandbox2.requests.RequestDecoder;
import sandbox2.responses.Response;
import sandbox2.responses.ResponseFactory;
import sandbox2.responses.ResponseEncoder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.*;

@ServerEndpoint(value = "/game", encoders = ResponseEncoder.class, decoders = RequestDecoder.class)
public class Endpoint {
	
	static Set<Session> peers = Collections.synchronizedSet(new HashSet<Session>());
	
	@OnOpen
	public void onOpen(Session session) {
		System.out.println("onOpen");
		peers.add(session);
	}
	
	@OnMessage
	public void onMessage(Request msg, Session session) {
		System.out.println("req: " + msg.getAction());
		Response response = ResponseFactory.create(msg.getAction());
		
		try {
			response.process(msg);
			String respAsc = new Gson().toJson(response);
			session.getBasicRemote().sendText(respAsc);
			
		} catch (JsonParseException e) {
			response.setRecoAndResponseText(0, "json parse exception");
		} catch (IOException e) {
			response.setRecoAndResponseText(0, "io exception");
		}
	}
	
	@OnClose
	public void onClose(Session session) {
		System.out.println("onClose");
	}

}
