package main.responses;

import javax.websocket.Session;

import lombok.Setter;
import main.database.StatsDao;
import main.processing.WorldProcessor;
import main.requests.AddExpRequest;
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
		if (!(req instanceof AddExpRequest))
			return;
		
		AddExpRequest request = (AddExpRequest)req;
		id = request.getId();
		statId = request.getStatId();
		exp = request.getExp();
		statShortName = StatsDao.getStatShortNameByStatId(statId);
		if (statId != -1)
			StatsDao.addExpToPlayer(request.getId(), statId, exp);
		
		responseMaps.addClientOnlyResponse(WorldProcessor.playerSessions.get(client), this);
	}

}
