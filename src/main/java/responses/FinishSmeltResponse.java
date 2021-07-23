package responses;

import java.util.Collections;
import java.util.List;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SmeltableDao;
import database.dao.StatsDao;
import database.dto.SmeltableDto;
import processing.attackable.Player;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.SmeltTaskUpdate;
import requests.AddExpRequest;
import requests.Request;
import requests.UseRequest;
import types.ItemAttributes;
import types.Stats;
import types.StorageTypes;

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
		
		new AddExpResponse().process(new AddExpRequest(player.getId(), Stats.SMITHING, smeltable.getExp()), player, responseMaps);
		
		TybaltsTaskManager.check(player, new SmeltTaskUpdate(smeltable.getBarId()), responseMaps);
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
	}

}
