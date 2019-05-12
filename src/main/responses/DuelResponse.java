package main.responses;

import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import main.requests.Request;
import main.state.Player;

public class DuelResponse extends PlayerResponse {

	public DuelResponse(String action) {
		super(action);
	}

}
