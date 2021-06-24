package main.responses;

import java.util.Collections;
import java.util.List;

import main.database.dao.ItemDao;
import main.database.dao.MineableDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SmeltableDao;
import main.database.dao.StatsDao;
import main.database.dto.SmeltableDto;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.AddExpRequest;
import main.requests.Request;
import main.requests.UseRequest;
import main.types.ItemAttributes;
import main.types.Stats;
import main.types.StorageTypes;

public class FinishSmeltResponse extends Response {
	
	public FinishSmeltResponse() {
		setAction("finish_smelt");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		UseRequest request = (UseRequest)req;
		
		final int srcItemId = request.getSrc();
		
		List<Integer> playerInvIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		SmeltableDto smeltable = SmeltableDao.getSmeltableByOreId(srcItemId, playerInvIds.contains(5));
		if (smeltable == null) {
			return;
		}
		
		// required ore check
		if (!playerInvIds.contains(srcItemId)) {
			return;
		}
		
		// level check
		final int smithingLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.SMITHING, player.getId());
		if (smithingLevel < smeltable.getLevel()) {
			return;
		}
		
		// required coal check
		final int playerCoalCount = Collections.frequency(playerInvIds, 5);
		if (playerCoalCount < smeltable.getRequiredCoal()) {
			return;
		}
		
		final int playerOreCount = PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), smeltable.getOreId(), StorageTypes.INVENTORY);
		if (playerOreCount < smeltable.getRequiredOre()) {
			return;
		}
		
		// replace the first primary ore with the bar, then remove the appropriate amount of coal.
		if (ItemDao.itemHasAttribute(smeltable.getOreId(), ItemAttributes.STACKABLE)) { // gold chips are stackable
			PlayerStorageDao.setCountOnSlot(player.getId(), StorageTypes.INVENTORY, playerInvIds.indexOf(smeltable.getOreId()), playerOreCount - smeltable.getRequiredOre());
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, smeltable.getBarId(), 1, ItemDao.getMaxCharges(smeltable.getBarId()));
		} else {
			int oreIndex = playerInvIds.indexOf(smeltable.getOreId());
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, oreIndex, smeltable.getBarId(), 1, 0);
			playerInvIds.set(oreIndex, 0);
			for (int i = 1; i < smeltable.getRequiredOre(); ++i) {
				oreIndex = playerInvIds.indexOf(smeltable.getOreId());
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, oreIndex, 0, 0, 0);
				playerInvIds.set(oreIndex, 0);
			}
		}
		for (int i = 0; i < smeltable.getRequiredCoal(); ++i) {
			int coalIndex = playerInvIds.indexOf(5);
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, coalIndex, 0, 0, 0);
			playerInvIds.set(coalIndex, 0);
		}
		
		new AddExpResponse().process(new AddExpRequest(player.getId(), Stats.SMITHING, smeltable.getLevel() * 10), player, responseMaps);
		
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
	}

}
