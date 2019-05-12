package main.responses;

import java.util.ArrayList;
import java.util.Map;

import javax.websocket.Session;

import com.google.gson.Gson;

import lombok.Setter;
import main.requests.Request;
import main.state.Player;


public class PlayerUpdateResponse extends Response {
	@Setter Integer id = null;
	@Setter String name = null;
	@Setter Integer cmb = null;
	@Setter Integer tile = null;
	@Setter String state = null;
	
	@Setter
	private class EquipUpdate {
		Integer head = null;
		Integer body = null;
		Integer legs = null;
		Integer shield = null;
		Integer sword = null;
	}

	public PlayerUpdateResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		return null;
	}

}
