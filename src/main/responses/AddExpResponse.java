package main.responses;

import lombok.Setter;
import main.database.StatsDao;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.requests.Request;

@Setter
public class AddExpResponse extends Response {
	
	private int id;
	private int statId;
	private String statShortName;
	private int exp;

	public AddExpResponse() {
		setAction("addexp");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof AddExpRequest))
			return;
		
		AddExpRequest request = (AddExpRequest)req;
		id = request.getId();
		statId = request.getStatId();
		exp = request.getExp();
		statShortName = StatsDao.getStatShortNameByStatId(statId);
		if (statId != -1)
			StatsDao.addExpToPlayer(request.getId(), statId, exp);
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
