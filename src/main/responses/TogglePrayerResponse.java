package main.responses;

import java.util.Set;

import lombok.Setter;
import main.database.PrayerDao;
import main.database.PrayerDto;
import main.processing.Player;
import main.requests.Request;
import main.requests.TogglePrayerRequest;
import main.types.Prayers;
import main.types.Stats;

public class TogglePrayerResponse extends Response {	
	@Setter private Set<Integer> activePrayers = null;
	
	public TogglePrayerResponse() {
		setAction("toggle_prayer");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof TogglePrayerRequest))
			return;
		
		TogglePrayerRequest request = (TogglePrayerRequest)req;
		PrayerDto prayer = PrayerDao.getPrayerById(request.getPrayerId());
		if (prayer == null) {
			// i guess they've tried to spoof the id, don't send them a response.
			return;
		}
		
		int playerPrayerLevel = player.getStats().get(Stats.PRAYER);
		if (playerPrayerLevel < prayer.getLevel()) {
			setRecoAndResponseText(0, String.format("you need %d prayer to activate this prayer.", prayer.getLevel()));
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (player.getPrayerPoints() == 0) {
			setRecoAndResponseText(0, "you need to recharge your prayer first.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		player.togglePrayer(Prayers.withValue(prayer.getId()));
		activePrayers = player.getActivePrayers();
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
