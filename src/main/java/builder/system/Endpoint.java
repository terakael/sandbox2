package builder.system;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;

import builder.requests.Request;
import builder.requests.RequestDecoder;
import builder.responses.Response;
import builder.responses.ResponseEncoder;
import builder.responses.ResponseFactory;

@ServerEndpoint(value = "/builder", encoders = ResponseEncoder.class, decoders = RequestDecoder.class)
public class Endpoint {
	private static Gson gson = new Gson();
	
	@OnOpen
	public void onOpen(Session session) {
		System.out.println("builder onOpen");
	}
	
	@OnMessage
	public void onMessage(Request msg, Session client) {
		System.out.println(msg.getAction());
		List<Response> responses = new LinkedList<>();
		
		final Response response = ResponseFactory.create(msg.getAction());
		response.process(msg, responses);
		
		try {
			client.getBasicRemote().sendText(gson.toJson(responses));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@OnClose
	public void onClose(Session session) {
		System.out.println("builder onClose");
	}
}

