package main.responses;

import main.database.dao.FishableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dto.FishableDto;
import main.processing.ConstructableManager;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.requests.FishRequest;
import main.requests.Request;
import main.requests.RequestFactory;
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
	}

}
