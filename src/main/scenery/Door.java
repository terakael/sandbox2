package main.scenery;

import main.database.dao.DoorDao;
import main.database.dto.LockedDoorDto;
import main.processing.LockedDoorManager;
import main.processing.Player;
import main.requests.UseRequest;
import main.responses.OpenCloseResponse;
import main.responses.PlayerUpdateResponse;
import main.responses.ResponseMaps;

public class Door implements Scenery {

	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		LockedDoorDto lockedDoor = LockedDoorManager.getLockedDoor(player.getFloor(), request.getDest());
		if (lockedDoor == null || lockedDoor.getUnlockItemId() == 0 || lockedDoor.getUnlockItemId() != request.getSrc()) {
			// not a locked door, or doesn't require an item to unlock, or it does require an item but not the item the user tried
			// return "nothing interesting happens".
			return false;
		}
		
		// the user used the correct item on teh door.
		if (LockedDoorManager.openLockedDoor(player.getFloor(), lockedDoor.getTileId())) {// if it's already open then keep it open; just reset the timer
			OpenCloseResponse openCloseResponse = new OpenCloseResponse();
			openCloseResponse.setTileId(lockedDoor.getTileId());
			responseMaps.addLocalResponse(player.getFloor(), lockedDoor.getTileId(), openCloseResponse);
		}
		
		// move the player to the other side
		int newPlayerTileId = LockedDoorManager.calculatePlayerNewTileId(player.getTileId(), lockedDoor.getTileId(), DoorDao.getDoorImpassableByTileId(player.getFloor(), lockedDoor.getTileId()));
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(player.getId());
		playerUpdate.setTileId(newPlayerTileId);
		player.setTileId(newPlayerTileId);
		responseMaps.addLocalResponse(player.getFloor(), lockedDoor.getTileId(), playerUpdate);
		
		return true;
	}

}
