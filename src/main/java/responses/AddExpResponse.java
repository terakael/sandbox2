package responses;

import java.util.HashMap;

import lombok.Setter;
import database.dao.StatsDao;
import processing.attackable.Player;
import requests.AddExpRequest;
import requests.Request;
import types.Stats;

@Setter
public class AddExpResponse extends Response {
	HashMap<Integer, Double> stats = new HashMap<>();

	public AddExpResponse() {
		setAction("addexp");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof AddExpRequest))
			return;
		
		AddExpRequest request = (AddExpRequest)req;
		if (request.getStatId() != -1)
			StatsDao.addExpToPlayer(player.getId(), Stats.withValue(request.getStatId()), request.getExp());
		
		addExp(request.getStatId(), request.getExp());
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
	public void addExp(int statId, double exp) {
		stats.put(statId, exp);
	}

}
