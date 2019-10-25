package main.responses;

import java.util.HashMap;
import java.util.Map;

import main.database.StatsDao;
import main.processing.Player;
import main.requests.Request;

public class StatBoostResponse extends Response {
	public StatBoostResponse() {
		setAction("stat_boosts");
	}
	private HashMap<Integer, Integer> boosts = new HashMap<>();

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		boosts = StatsDao.getRelativeBoostsByPlayerId(player.getId());
		responseMaps.addClientOnlyResponse(player, this);
	}

}
