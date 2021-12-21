package system;
import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;

import requests.Request;
import requests.RequestDecoder;
import responses.CachedResourcesResponse;
import responses.ResponseEncoder;

@ServerEndpoint(value = "/resources", encoders = ResponseEncoder.class, decoders = RequestDecoder.class)
public class ResourceEndpoint {
	private static Gson gson = new Gson();
	
	@OnOpen
	public void onOpen(Session session) {
		System.out.println("client connected and requested resources");
	}
	
	@OnMessage
	public void onMessage(Request msg, Session client) {
		if (msg.getAction().equals("resources")) { // only one command	
			try {
				client.getBasicRemote().sendText(gson.toJson(CachedResourcesResponse.get()));
				client.close();// automatically close the connection once the resources have been sent.
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@OnClose
	public void onClose(Session session) {
		System.out.println("closed resource connection");
	}
}
