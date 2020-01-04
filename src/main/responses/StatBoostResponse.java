package main.responses;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import main.database.StatsDao;
import main.processing.Player;
import main.requests.Request;
import main.types.Stats;

public class StatBoostResponse extends Response {
	public StatBoostResponse() {
		setAction("stat_boosts");
	}
	private Map<Integer, Integer> boosts = new HashMap<>();

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		boosts = StatsDao.getRelativeBoostsByPlayerId(player.getId())
				.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey().getValue(), Map.Entry::getValue));
		responseMaps.addClientOnlyResponse(player, this);
	}

}
