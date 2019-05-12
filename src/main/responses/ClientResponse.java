package main.responses;

import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import main.requests.Request;
import main.state.Player;

public class ClientResponse extends Response {
	
	private ArrayList<MessageResponse> message_responses = new ArrayList<>();
	private ArrayList<PlayerUpdateResponse> player_updates = new ArrayList<>();
	private ArrayList<Response> responses = new ArrayList<>();
	
	public void addMessageResponse(MessageResponse response) {
		message_responses.add(response);
	}
	
	public void addPlayerUpdate(PlayerUpdateResponse response) {
		player_updates.add(response);
	}
	
	public void addResponse(Response response) {
		responses.add(response);
	}

	public ClientResponse(String action) {
		super(action);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		return null;
	}

}
