package responses;

import java.util.List;
import java.util.Map;

import database.dao.BuryableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.StatsDao;
import processing.attackable.Player;
import requests.BuryRequest;
import requests.Request;
import types.Stats;
import types.StorageTypes;

public class BuryResponse extends Response {
	public BuryResponse() {
		setAction("bury");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof BuryRequest))
			return;
		
		BuryRequest request = (BuryRequest)req;
		
		// check if there's a buryable at this place
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		int boneId = invItemIds.get(request.getSlot());
		if (!BuryableDao.isBuryable(boneId)) {
			// it's not a bone, you can't bury this.
			return;
		}
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), 0, 1, 0);
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		setResponseText(String.format("you bury the %s.", ItemDao.getNameFromId(boneId, false)));
		responseMaps.addClientOnlyResponse(player, this);
		
		handleBury(player, boneId, responseMaps);
	}

	public static void handleBury(Player player, int boneId, ResponseMaps responseMaps) {
		int prayerLevelBeforeBury = StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, player.getId());
		StatsDao.addExpToPlayer(player.getId(), Stats.PRAYER, BuryableDao.getExpFromBuryable(boneId));
		int prayerLevelAfterBury = StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, player.getId());
		
		AddExpResponse addExpResponse = new AddExpResponse();
		addExpResponse.addExp(Stats.PRAYER.getValue(), BuryableDao.getExpFromBuryable(boneId));
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
				PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
				playerUpdate.setId(player.getId());
				playerUpdate.setCombatLevel(afterPrayerLevelCombat);
				responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), playerUpdate);
			}
		}
	}
}
