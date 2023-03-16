package responses;

import java.util.Set;

import lombok.Setter;
import database.dao.PrayerDao;
import database.dto.PrayerDto;
import processing.attackable.Player;
import processing.managers.FightManager;
import processing.managers.FightManager.Fight;
import requests.Request;
import requests.TogglePrayerRequest;
import types.DuelRules;
import types.Prayers;
import types.Stats;

public class TogglePrayerResponse extends Response {	
	@Setter private Set<Integer> activePrayers = null;
	
	public TogglePrayerResponse() {
		setAction("toggle_prayer");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
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
