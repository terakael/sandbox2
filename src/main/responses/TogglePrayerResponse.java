package main.responses;

import java.util.Set;

import lombok.Setter;
import main.database.PrayerDao;
import main.database.PrayerDto;
import main.processing.FightManager;
import main.processing.FightManager.Fight;
import main.processing.Player;
import main.requests.Request;
import main.requests.TogglePrayerRequest;
import main.types.DuelRules;
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
		
		Fight fight = FightManager.getFightByPlayerId(player.getId());
		if (fight != null && fight.getRules() != null && (fight.getRules() & DuelRules.no_prayer.getValue()) > 0) {
			setRecoAndResponseText(1, "prayers are disabled for this duel.");
			responseMaps.addClientOnlyResponse(player, this);
			
			// just in case they somehow activated a prayer.  This does nothing if there's no prayers activated.
			player.clearActivePrayers(responseMaps);
			return;
		}
		
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
