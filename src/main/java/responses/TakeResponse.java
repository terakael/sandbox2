package responses;

import java.util.List;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import processing.managers.RoomGroundItemManager;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.TakeTaskUpdate;
import requests.Request;
import requests.TakeRequest;
import system.GroundItemManager;
import types.ItemAttributes;
import types.StorageTypes;

public class TakeResponse extends WalkAndDoResponse {
	private transient RoomGroundItemManager.GroundItem groundItem = null;

	@Override
	protected boolean setTarget(Request request, Player player, ResponseMaps responseMaps) {
		TakeRequest takeReq = (TakeRequest)request;
		groundItem = GroundItemManager.getItemAtTileId(player.getFloor(), player.getId(), takeReq.getItemId(), takeReq.getTileId());
		if (groundItem == null)
			return false;
		
		walkingTargetTileId = takeReq.getTileId();
		return true;
	}
	
	@Override
	protected boolean nextToTarget(Request request, Player player, ResponseMaps responseMaps) {
		return walkingTargetTileId == player.getTileId();
	}
	
	@Override
	protected void walkToTarget(Request request, Player player, ResponseMaps responseMaps) {
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), walkingTargetTileId, true));
	}

	@Override
	protected void doAction(Request request, Player player, ResponseMaps responseMaps) {
		List<Integer> invItems = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (ItemDao.itemHasAttribute(groundItem.getId(), ItemAttributes.STACKABLE)) {
			if (!invItems.contains(0) && !invItems.contains(groundItem.getId())) {
				setRecoAndResponseText(0, "you don't have enough space to take that.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			int invItemIndex = invItems.indexOf(groundItem.getId());
			if (invItemIndex >= 0) {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY, invItemIndex, groundItem.getCount());
			} else {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, groundItem.getId(), groundItem.getCount(), 0);
			}
		} else {
			if (!invItems.contains(0)) {
				setRecoAndResponseText(0, "you don't have enough space to take that.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, groundItem.getId(), 1, groundItem.getCharges());
		}
		
		GroundItemManager.remove(player.getFloor(), player.getId(), request.getTileId(), groundItem.getId(), groundItem.getCount(), groundItem.getCharges());
		TybaltsTaskManager.check(player, new TakeTaskUpdate(groundItem.getId(), groundItem.getCount()), responseMaps);
		
		// update the player inventory/equipped items and only send it to the player
		Response resp = ResponseFactory.create("invupdate");
		resp.process(request, player, responseMaps);// adds itself to the appropriate responseMap
		player.setState(PlayerState.idle);
	}

}
