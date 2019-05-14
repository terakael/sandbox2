package main.responses;

import javax.websocket.Session;

import lombok.Setter;
import main.requests.Request;

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
	public void process(Request req, Session client, ResponseMaps responseMaps) {

	}

}
