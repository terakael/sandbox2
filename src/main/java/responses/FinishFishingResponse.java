package responses;

import java.util.List;

import database.dao.FishableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.FishableDto;
import processing.WorldProcessor;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import processing.managers.ConstructableManager;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.FishTaskUpdate;
import requests.AddExpRequest;
import requests.FishRequest;
import requests.Request;
import requests.RequestFactory;
import types.SceneryAttributes;
import types.Stats;
import types.StorageTypes;
import utils.RandomUtil;

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
		
		setResponseText(String.format("you catch some %s.", ItemDao.getNameFromId(fishable.getItemId(), false)));
		responseMaps.addClientOnlyResponse(player, this);
		
		TybaltsTaskManager.check(player, new FishTaskUpdate(fishable.getItemId()), responseMaps);
		ArtisanManager.check(player, fishable.getItemId(), responseMaps);
	}

}
