package main.responses;

import main.database.ItemDao;
import main.database.PickableDao;
import main.database.PickableDto;
import main.database.PlayerStorageDao;
import main.processing.FlowerManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.RockManager;
import main.processing.Player.PlayerState;
import main.requests.PickRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.StorageTypes;

public class PickResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof PickRequest))
			return;
		
		PickRequest request = (PickRequest)req;
		if (!PathFinder.isNextTo(player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			PickableDto pickable = PickableDao.getPickableByTileId(request.getTileId());
			if (pickable == null) {
				setRecoAndResponseText(0, "you can't pick that.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			if (FlowerManager.flowerIsDepleted(request.getTileId())) {
				setRecoAndResponseText(1, "you need to wait until it grows back.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			int freeSlotId = PlayerStorageDao.getFreeSlotByPlayerId(player.getId());
			if (freeSlotId == -1) {
				setRecoAndResponseText(0, "your inventory is full.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			setRecoAndResponseText(1, String.format("you pick some %s.", ItemDao.getNameFromId(pickable.getItemId())));
			responseMaps.addClientOnlyResponse(player, this);
			
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), freeSlotId, pickable.getItemId(), 1);
			new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
			
			FlowerManager.addDepletedFlower(request.getTileId(), pickable.getRespawnTicks());
			
			FlowerDepleteResponse flowerDepleteResponse = new FlowerDepleteResponse();
			flowerDepleteResponse.setTileId(request.getTileId());
			responseMaps.addBroadcastResponse(flowerDepleteResponse);
		}
	}

}