package responses;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import database.dao.StatsDao;
import processing.attackable.Player;
import requests.Request;

@SuppressWarnings("unused")
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
