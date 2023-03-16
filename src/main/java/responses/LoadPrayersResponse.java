package responses;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.dao.PrayerDao;
import database.dto.PrayerDto;
import processing.attackable.Player;
import requests.Request;
import types.Stats;

public class LoadPrayersResponse extends Response {
	private List<PrayerDto> prayers = null;
	
	public LoadPrayersResponse() {
		setAction("load_prayers");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
		Set<Integer> replacementPrayers = new HashSet<>();
		for (Map.Entry<Integer, List<Integer>> entry : PrayerDao.getPrayerReplacements().entrySet()) {
			replacementPrayers.add(entry.getKey());
			for (Integer val : entry.getValue())
				replacementPrayers.add(val);
		}
		
		// all the prayers excluding the potential replacement prayers; we'll add the correct ones in after
		prayers = PrayerDao.getPrayers().values().stream().filter(e -> !replacementPrayers.contains(e.getId())).collect(Collectors.toList());
		
		final int prayerLevel = player.getStats().get(Stats.PRAYER);
		prayers.addAll(PrayerDao.getReplacementPrayersByPrayerLevel(prayerLevel));
		
		prayers.sort(Comparator.comparing(PrayerDto::getLevel));

		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
