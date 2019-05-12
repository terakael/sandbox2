package main.responses;

import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import lombok.Setter;
import main.requests.Request;
import main.state.Player;

@Setter
public class AddExpResponse extends Response {
	
	private int id;
	private int statId;
	private String statShortName;
	private int exp;

	public AddExpResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		return ResponseType.client_only;
	}

}
