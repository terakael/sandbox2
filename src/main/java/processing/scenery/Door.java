package processing.scenery;

import database.dao.DoorDao;
import database.dao.PlayerStorageDao;
import database.dto.LockedDoorDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.managers.LockedDoorManager;
import requests.UseRequest;
import responses.InventoryUpdateResponse;
import responses.OpenCloseResponse;
import responses.PlayerUpdateResponse;
import responses.ResponseMaps;
import types.StorageTypes;

public class Door implements Scenery {

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		LockedDoorDto lockedDoor = LockedDoorManager.getLockedDoor(player.getFloor(), request.getDest());
		if (lockedDoor == null || lockedDoor.getUnlockItemId() == 0 || lockedDoor.getUnlockItemId() != request.getSrc()) {
			// not a locked door, or doesn't require an item to unlock, or it does require an item but not the item the user tried
			// return "nothing interesting happens".
			return false;
		}
		
		// the tile the player would end up
		final int newPlayerTileId = PathFinder.calculateThroughTileId(lockedDoor.getTileId(), DoorDao.getDoorImpassableByTileId(player.getFloor(), lockedDoor.getTileId()));
		if (newPlayerTileId == -1 || newPlayerTileId == lockedDoor.getTileId())
			return false; // fail early if we don't go through properly to prevent potentially destroying the inventory item 
		
		// destroy use-item if necessary
		if (lockedDoor.isDestroyOnUse()) {
			int slotId = PlayerStorageDao.getSlotOfItemId(player.getId(), StorageTypes.INVENTORY, lockedDoor.getUnlockItemId());
			if (slotId == -1)
				return false; // they don't have the item?
			
			PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY, slotId, -1);
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
		}
		
		// the user used the correct item on teh door.
		if (LockedDoorManager.openLockedDoor(player.getFloor(), lockedDoor.getTileId())) {// if it's already open then keep it open; just reset the timer
			OpenCloseResponse openCloseResponse = new OpenCloseResponse();
			openCloseResponse.setTileId(lockedDoor.getTileId());
			responseMaps.addLocalResponse(player.getFloor(), lockedDoor.getTileId(), openCloseResponse);
		}
		
		player.setTileId(newPlayerTileId);
		
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(player.getId());
		playerUpdate.setTileId(newPlayerTileId);
		responseMaps.addLocalResponse(player.getFloor(), lockedDoor.getTileId(), playerUpdate);
		
		return true;
	}

}
