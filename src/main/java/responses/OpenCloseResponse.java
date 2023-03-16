package responses;

import java.util.List;
import java.util.Stack;

import database.dao.DoorDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.DoorDto;
import database.dto.LockedDoorDto;
import lombok.Setter;
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
		tileId = request.getTileId();
		
		// does the tile a door on it?
		DoorDto door = DoorDao.getDoorDtoByTileId(player.getFloor(), tileId);
		if (door == null) {
			// this can technically happen when the player clicks a ladder, then right-clicks a door
			// then finally selects "open" after they have switched rooms.  Do not do anything in this case.
			return;
		}
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		final int impassable = DoorDao.getDoorImpassableByTileId(player.getFloor(), tileId);
		int throughTileId = PathFinder.calculateThroughTileId(tileId, impassable);
		if (throughTileId == -1)
			return; // multiple sides means we can't deduce where to walk through
		
		// we want to start by walking to the closest tile to us (i.e. the side of the wall we're on)
		final int closestTileId = PathFinder.getCloserTile(player.getTileId(), tileId, throughTileId);
		
		final LockedDoorDto lockedDoor = LockedDoorManager.getLockedDoor(player.getFloor(), tileId);
		
		// if it's not a locked door, we want to check "next to", because we need to be able to open/close from either side.
		// if it is a locked door, then we want to check if we're exactly on the closest tile to prevent speed-walking
		final boolean closeEnoughToOpen = lockedDoor == null
				? PathFinder.isNextTo(player.getFloor(), player.getTileId(), closestTileId)
				: player.getTileId() == closestTileId;
		
		if (!closeEnoughToOpen) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), closestTileId, true));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {			
			player.faceDirection(request.getTileId(), responseMaps);
			if (lockedDoor != null) {
				String failedRequirementReason = LockedDoorManager.playerMeetsDoorRequirements(player, lockedDoor);
				if (failedRequirementReason.isEmpty()) { // empty reason means the player meets the requirements
					if (LockedDoorManager.openLockedDoor(player.getFloor(), tileId)) // if it's already open then keep it open; just reset the timer
						responseMaps.addLocalResponse(player.getFloor(), tileId, this);
					// move the player to the other side
					// our destination tile is the tile that isn't closest, i.e. the one on the other side of the wall
					final int newPlayerTileId = throughTileId == closestTileId ? request.getTileId() : throughTileId;
					
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
