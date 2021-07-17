package main.processing.scenery;

import java.util.Map;

import main.database.dao.BuryableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.StatsDao;
import main.processing.Player;
import main.requests.UseRequest;
import main.responses.AddExpResponse;
import main.responses.InventoryUpdateResponse;
import main.responses.MessageResponse;
import main.responses.PlayerUpdateResponse;
import main.responses.ResponseMaps;
import main.types.Stats;
import main.types.StorageTypes;

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
