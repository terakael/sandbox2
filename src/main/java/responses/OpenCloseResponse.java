package responses;

import java.util.List;
import java.util.Stack;

import lombok.Setter;
import database.dao.DoorDao;
import database.dao.PlayerStorageDao;
import database.dto.DoorDto;
import database.dto.LockedDoorDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import processing.managers.LockedDoorManager;
import requests.OpenRequest;
import requests.Request;
import types.StorageTypes;

public class OpenCloseResponse extends Response {
	@Setter private int tileId;
	
	public OpenCloseResponse() {
		setAction("open");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		OpenRequest request = (OpenRequest)req;
		
		// does the tile a door on it?
		DoorDto door = DoorDao.getDoorDtoByTileId(player.getFloor(), request.getTileId());
		if (door == null) {
			// this can technically happen when the player clicks a ladder, then right-clicks a door
			// then finally selects "open" after they have switched rooms.  Do not do anything in this case.
			return;
		}
		tileId = request.getTileId();
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), tileId, false, true)) {
			Stack<Integer> path = PathFinder.findPathToDoor(player.getFloor(), player.getTileId(), tileId);
			// empty check because it's not guaranteed that there is a path between the player and the door.
			if (!path.isEmpty()) {
				player.setPath(path);
				player.setState(PlayerState.walking);
				player.setSavedRequest(req);
			}
			return;
		} else {			
			player.faceDirection(request.getTileId(), responseMaps);
			
			LockedDoorDto lockedDoor = LockedDoorManager.getLockedDoor(player.getFloor(), tileId);
			if (lockedDoor != null) {
				String failedRequirementReason = LockedDoorManager.playerMeetsDoorRequirements(player, lockedDoor);
				if (failedRequirementReason.isEmpty()) { // empty reason means the player meets the requirements
					if (LockedDoorManager.openLockedDoor(player.getFloor(), tileId)) // if it's already open then keep it open; just reset the timer
						responseMaps.addLocalResponse(player.getFloor(), tileId, this);
					// move the player to the other side
					int newPlayerTileId = LockedDoorManager.calculatePlayerNewTileId(player.getTileId(), tileId, DoorDao.getDoorImpassableByTileId(player.getFloor(), tileId));
					PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
					playerUpdate.setId(player.getId());
					playerUpdate.setTileId(newPlayerTileId);
					player.setTileId(newPlayerTileId);
					responseMaps.addLocalResponse(player.getFloor(), tileId, playerUpdate);
					
					// sometimes a key should be used only once, and removed from inventory on use.
					if (lockedDoor.isDestroyOnUse()) {
						List<Integer> playerInvIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
						int slot = playerInvIds.indexOf(lockedDoor.getUnlockItemId());
						if (slot != -1) {
							PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, 0, 0, 0);
							InventoryUpdateResponse.sendUpdate(player, responseMaps);
						}
					}
				} else {
					setRecoAndResponseText(0, failedRequirementReason);
					responseMaps.addClientOnlyResponse(player, this);
				}
			} else {
				DoorDao.toggleDoor(player.getFloor(), tileId);
				responseMaps.addLocalResponse(player.getFloor(), tileId, this);
			}
		}
	}
}