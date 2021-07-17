package main.responses;

import java.util.List;

import main.database.dao.FishableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.database.dto.FishableDto;
import main.processing.ConstructableManager;
import main.processing.Player;
import main.processing.TybaltsTaskManager;
import main.processing.WorldProcessor;
import main.processing.tybaltstasks.updates.FishTaskUpdate;
import main.requests.AddExpRequest;
import main.requests.FishRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.SceneryAttributes;
import main.types.Stats;
import main.types.StorageTypes;
import main.utils.RandomUtil;

public class FinishFishingResponse extends Response {
	
	public FinishFishingResponse() {
		setAction("finish_fishing");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof FishRequest))
			return;
		
		FishRequest request = (FishRequest)req;
		FishableDto fishable = FishableDao.getFishableDtoByTileId(request.getTileId());
		if (fishable == null) {
			FishResponse fishResponse = new FishResponse();
			fishResponse.setRecoAndResponseText(0, "you can't fish that.");
			responseMaps.addClientOnlyResponse(player, fishResponse);
			return;
		}
		
		// sometimes scenery only appears at night or day
		final boolean isDiurnal = SceneryDao.sceneryContainsAttribute(fishable.getSceneryId(), SceneryAttributes.DIURNAL);
		final boolean isNocturnal = SceneryDao.sceneryContainsAttribute(fishable.getSceneryId(), SceneryAttributes.NOCTURNAL);
		if ((WorldProcessor.isDaytime() && !isDiurnal) || (!WorldProcessor.isDaytime() && !isNocturnal))
			return;
		
		if (fishable.getBaitId() != 0) {
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			int baitSlot = invItemIds.indexOf(fishable.getBaitId());
			if (baitSlot == -1)
				return; // shouldn't happen as it's been checked in the FishResponse
			
			int remainingBait = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, baitSlot).getCount();
			PlayerStorageDao.setCountOnSlot(player.getId(), StorageTypes.INVENTORY, baitSlot, remainingBait - 1);
		}
		
		PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, fishable.getItemId(), 1, ItemDao.getMaxCharges(fishable.getItemId()));
		
		// 20% chance to catch a double fish if near a fishing totem pole
		if (ConstructableManager.constructableIsInRadius(player.getFloor(), player.getTileId(), 137, 3) && RandomUtil.chance(20)) {
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, fishable.getItemId(), 1, ItemDao.getMaxCharges(fishable.getItemId()));
		}
		
		AddExpRequest addExpReq = new AddExpRequest();
		addExpReq.setStatId(Stats.FISHING.getValue());
		addExpReq.setExp(fishable.getExp());
		
		new AddExpResponse().process(addExpReq, player, responseMaps);
		new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
		
		setResponseText(String.format("you catch some %s.", ItemDao.getNameFromId(fishable.getItemId())));
		responseMaps.addClientOnlyResponse(player, this);
		
		TybaltsTaskManager.check(player, new FishTaskUpdate(fishable.getItemId()), responseMaps);
	}

}
