package processing.scenery;

import java.util.Map;

import database.dao.BuryableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.StatsDao;
import processing.attackable.Player;
import requests.UseRequest;
import responses.AddExpResponse;
import responses.InventoryUpdateResponse;
import responses.MessageResponse;
import responses.PlayerUpdateResponse;
import responses.ResponseMaps;
import types.Stats;
import types.StorageTypes;

public class Altar implements Scenery {

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final int srcItemId = request.getSrc();
		if (!BuryableDao.isBuryable(srcItemId))
			return false;
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), 0, 1, 0);
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		MessageResponse messageResponse = new MessageResponse();
		messageResponse.setResponseText(String.format("you sacrifice the %s at the altar.", ItemDao.getNameFromId(srcItemId)));
		messageResponse.setColour("white");
		responseMaps.addClientOnlyResponse(player, messageResponse);
		
		int prayerLevelBeforeBury = StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, player.getId());
		StatsDao.addExpToPlayer(player.getId(), Stats.PRAYER, BuryableDao.getExpFromBuryable(srcItemId) * 2);
		int prayerLevelAfterBury = StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, player.getId());
		
		AddExpResponse addExpResponse = new AddExpResponse();
		addExpResponse.addExp(Stats.PRAYER.getValue(), BuryableDao.getExpFromBuryable(srcItemId) * 2);
		responseMaps.addClientOnlyResponse(player, addExpResponse);
		
		if (prayerLevelAfterBury > prayerLevelBeforeBury) {
			player.setPrayerPoints(player.getPrayerPoints() + 1, responseMaps);
			
			// we gained a prayer level - if it causes a combat level then send a combat level update
			Map<Stats, Integer> stats = player.getStats();
			stats.put(Stats.PRAYER, prayerLevelBeforeBury);
			int beforePrayerLevelCombat = StatsDao.getCombatLevelByStats(stats);
			
			stats.put(Stats.PRAYER, prayerLevelAfterBury);
			int afterPrayerLevelCombat = StatsDao.getCombatLevelByStats(stats);
			
			if (afterPrayerLevelCombat > beforePrayerLevelCombat) {
				// the prayer level resulted in a combat level!  send a local response.
				PlayerUpdateResponse playerUpdateCmb = new PlayerUpdateResponse();
				playerUpdateCmb.setId(player.getId());
				playerUpdateCmb.setCombatLevel(afterPrayerLevelCombat);
				responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), playerUpdateCmb);
			}
		}

		return true;
	}

}
