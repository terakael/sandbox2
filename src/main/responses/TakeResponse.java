package main.responses;

import java.util.List;

import main.GroundItemManager;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.processing.RoomGroundItemManager;
import main.requests.Request;
import main.requests.TakeRequest;
import main.types.ItemAttributes;
import main.types.StorageTypes;

public class TakeResponse extends Response {
	//@Setter private List<GroundItemManager.GroundItem> groundItems;

	public TakeResponse() {
		setAction("take");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof TakeRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		TakeRequest takeReq = (TakeRequest)req;
		
		RoomGroundItemManager.GroundItem groundItem = GroundItemManager.getItemAtTileId(player.getFloor(), player.getId(), takeReq.getItemId(), takeReq.getTileId());
		if (groundItem == null) {
//			setRecoAndResponseText(0, "too late - it's gone!");
//			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (takeReq.getTileId() != player.getTileId()) {
			// walk over to the item before picking it up
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), takeReq.getTileId(), true));
			player.setState(PlayerState.walking);
			
			// save the request so the player reprocesses it when they arrive at their destination
			player.setSavedRequest(req);
			return;
		}
		
		List<Integer> invItems = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (ItemDao.itemHasAttribute(groundItem.getId(), ItemAttributes.STACKABLE)) {
			if (!invItems.contains(0) && !invItems.contains(takeReq.getItemId())) {
				setRecoAndResponseText(0, "you don't have enough space to take that.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			int invItemIndex = invItems.indexOf(groundItem.getId());
			if (invItemIndex >= 0) {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY, invItemIndex, groundItem.getCount());
			} else {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, takeReq.getItemId(), groundItem.getCount(), 0);
			}
		} else {
			if (!invItems.contains(0)) {
				setRecoAndResponseText(0, "you don't have enough space to take that.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, takeReq.getItemId(), 1, groundItem.getCharges());
		}
		
		GroundItemManager.remove(player.getFloor(), player.getId(), takeReq.getTileId(), takeReq.getItemId(), groundItem.getCount(), groundItem.getCharges());
		
		// update the player inventory/equipped items and only send it to the player
		Response resp = ResponseFactory.create("invupdate");
		resp.process(takeReq, player, responseMaps);// adds itself to the appropriate responseMap
		player.setState(PlayerState.idle);
	}

}
