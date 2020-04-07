package main.responses;

import java.util.HashMap;

import lombok.Setter;
import main.database.StatsDao;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.requests.Request;
import main.types.Stats;

@Setter
public class AddExpResponse extends Response {
	HashMap<Integer, Double> stats = new HashMap<>();

	public AddExpResponse() {
		setAction("addexp");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof AddExpRequest))
			return;
		
		AddExpRequest request = (AddExpRequest)req;
		if (request.getStatId() != -1)
			StatsDao.addExpToPlayer(request.getId(), Stats.withValue(request.getStatId()), request.getExp());
		
		addExp(request.getStatId(), request.getExp());
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
	public void addExp(int statId, double exp) {
		stats.put(statId, exp);
	}

}
