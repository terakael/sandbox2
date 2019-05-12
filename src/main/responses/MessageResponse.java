package main.responses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.websocket.Session;

import lombok.Setter;
import main.database.PlayerDao;
import main.database.StatsDao;
import main.processing.WorldProcessor;
import main.requests.MessageRequest;
import main.requests.Request;
import main.responses.Response.ResponseType;
import main.state.Player;

public class MessageResponse extends Response {
	private String name;
	private int id;
	@Setter private String message;
	@Setter private String colour = "yellow";

	public MessageResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof MessageRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		Player player = WorldProcessor.playerSessions.get(client);
		
		MessageRequest messageReq = (MessageRequest)req;
		setRecoAndResponseText(1, "");
		
		String msg = messageReq.getMessage();
		id = messageReq.getId();
		
		if (msg.length() >= 2 && msg.substring(0, 2).equals("::"))
			return handleDebugCommand(id, msg, client);
		
		if (msg.length() > 100)
			msg = msg.substring(0, 100);
		
		name = PlayerDao.getNameFromId(id);
		message = msg;
		
		responseMaps.addLocalResponse(player, this);
		return ResponseType.broadcast;// TODO change to ResponseType.local
	}
	
	private ResponseType handleDebugCommand(int playerId, String msg, Session client) {
		Pattern gainExp = Pattern.compile("^::(att|str|def|hp|agil|acc|mage|herb|mine|smith) (-?\\d+)$");
		Matcher matcher = gainExp.matcher(msg);
		if (matcher.find() ) {
			String stat = matcher.group(1);
			int exp = Integer.parseInt(matcher.group(2));
			
			int statId = StatsDao.getStatIdByName(stat);
			if (statId != -1)
				StatsDao.addExpToPlayer(playerId, statId, exp);
			
			AddExpResponse resp = new AddExpResponse("addexp");
			resp.setId(playerId);
			resp.setStatId(statId);
			resp.setStatShortName(stat);
			resp.setExp(exp);
			
			try {
				client.getBasicRemote().sendText(gson.toJson(resp));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ResponseType.no_response;
	}

}
