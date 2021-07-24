package responses;

import java.util.List;

import database.dao.ChoppableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.ChoppableDto;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import processing.managers.DepletionManager;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.ChopTaskUpdate;
import requests.AddExpRequest;
import requests.ChopRequest;
import requests.Request;
import types.Stats;
import types.StorageTypes;
import utils.RandomUtil;

public class FinishChopResponse extends Response {
	private static final int hatchetId = ItemDao.getIdFromName("hatchet");
	private static final int goldenHatchetId = ItemDao.getIdFromName("golden hatchet");

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		ChopRequest request = (ChopRequest)req;
		
		final int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (sceneryId == -1) {
			// tileId doesn't have scenery, this is client fuckery
			return;
		}
		
		ChoppableDto choppable = ChoppableDao.getChoppableBySceneryId(sceneryId);
		if (choppable == null) {
			// this scenery isn't choppable
			setRecoAndResponseText(0, "you can't chop that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (DepletionManager.isDepleted(DepletionManager.DepletionType.tree, player.getFloor(), request.getTileId())) {
			setRecoAndResponseText(0, "the tree has been cut down.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		List<Integer> inventoryItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!inventoryItemIds.contains(hatchetId) && !inventoryItemIds.contains(goldenHatchetId)) {
			setRecoAndResponseText(0, "you need a hatchet in order to chop the tree.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (choppable.getSceneryId() == SceneryDao.getIdByName("magic tree") && !inventoryItemIds.contains(goldenHatchetId)) {
			setRecoAndResponseText(0, "this hatchet doesn't seem powerful enough to chop this tree.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		final int woodcuttingLevel = player.getStats().get(Stats.WOODCUTTING);
		final int requirementLevel = choppable.getLevel();
		
		// when woodcutting level and requirement level are equal, then there's a base X% chance of success depending on tree.
		// every subsequent level is another 1% chance of success.
		final int baseChance = 50 - (requirementLevel/2); 
		final int chance = Math.min((woodcuttingLevel - requirementLevel) + baseChance, 100);
		if (RandomUtil.chance(chance)) {
			// success
			if (inventoryItemIds.contains(goldenHatchetId))
				decreaseHatchetCharge(player.getId(), inventoryItemIds, goldenHatchetId);
			
			AddExpRequest addExpReq = new AddExpRequest();
			addExpReq.setStatId(Stats.WOODCUTTING.getValue());
			addExpReq.setExp(choppable.getExp());
			
			new AddExpResponse().process(addExpReq, player, responseMaps);
			
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, choppable.getLogId(), 1, ItemDao.getMaxCharges(choppable.getLogId()));
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
			TybaltsTaskManager.check(player, new ChopTaskUpdate(choppable.getSceneryId()), responseMaps);
			ArtisanManager.check(player, choppable.getLogId(), responseMaps);
			
			// flat 20% chance of depletion
			if (RandomUtil.chance(20)) {
				DepletionManager.addDepletedScenery(DepletionManager.DepletionType.tree, player.getFloor(), request.getTileId(), choppable.getRespawnTicks(), responseMaps);
			}
		}
	}
	
	private void decreaseHatchetCharge(int playerId, List<Integer> invItemIds, int hatchetId) {
		InventoryItemDto invItem = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(playerId, StorageTypes.INVENTORY, invItemIds.indexOf(hatchetId));
		if (invItem.getCharges() > 1) {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(
					playerId, 
					StorageTypes.INVENTORY, 
					invItemIds.indexOf(hatchetId), 
					hatchetId, 1, invItem.getCharges() - 1);
		} else {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(
					playerId, StorageTypes.INVENTORY, invItemIds.indexOf(hatchetId), ItemDao.getDegradedItemId(hatchetId), 1, 0);
		}
	}

}
