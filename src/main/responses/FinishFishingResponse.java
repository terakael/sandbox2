package main.responses;

import lombok.Setter;
import main.database.FishableDao;
import main.database.FishableDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.AddExpRequest;
import main.requests.FishRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.Stats;
import main.types.StorageTypes;

public class FinishFishingResponse extends Response {
	@Setter private int tileId;
	
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
		
		tileId = request.getTileId();// the tile we just finished fishing
		
		PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), fishable.getItemId(), 1, ItemDao.getMaxCharges(fishable.getItemId()));
		
		AddExpRequest addExpReq = new AddExpRequest();
		addExpReq.setId(player.getId());
		addExpReq.setStatId(Stats.FISHING.getValue());
		addExpReq.setExp(fishable.getExp());
		
		new AddExpResponse().process(addExpReq, player, responseMaps);
		new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
		
		setResponseText(String.format("you catch some %s.", ItemDao.getNameFromId(fishable.getItemId())));
		responseMaps.addClientOnlyResponse(player, this);
	}

}
