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

public class OpenCloseResponse extends WalkAndDoResponse {
	@Setter private int tileId;
	
	// xcom between functions
	private transient LockedDoorDto lockedDoor = null;
	private transient int throughTileId = -1;
	
	public OpenCloseResponse() {
		setAction("open");
	}
	
	@Override
	protected boolean setTarget(Request request, Player player, ResponseMaps responseMaps) {
		tileId = request.getTileId();
		
		if (DoorDao.getDoorDtoByTileId(player.getFloor(), tileId) == null) {
			// this can technically happen when the player clicks a ladder, then right-clicks a door
			// then finally selects "open" after they have switched rooms.  Do not do anything in this case.
			return false;
		}
		
		final int impassable = DoorDao.getDoorImpassableByTileId(player.getFloor(), tileId);
		throughTileId = PathFinder.calculateThroughTileId(tileId, impassable);
		if (throughTileId == -1)
			return false; // multiple sides means we can't deduce where to walk through
		
		// we want to start by walking to the closest tile to us (i.e. the side of the wall we're on)
		walkingTargetTileId = PathFinder.getCloserTile(player.getTileId(), tileId, throughTileId);
		
		// if we're around a corner from a door then the inside of a closed door could be the closest tile.
		// we need to check if we can actually move to the tile.
		if (walkingTargetTileId != player.getTileId() && PathFinder.findPath(player.getFloor(), player.getTileId(), walkingTargetTileId, true).isEmpty())
			walkingTargetTileId = walkingTargetTileId == tileId ? throughTileId : tileId;
		
		lockedDoor = LockedDoorManager.getLockedDoor(player.getFloor(), tileId);
		
		return true;
	}
	
	@Override
	protected boolean nextToTarget(Request request, Player player, ResponseMaps responseMaps) {
		// if it's not a locked door, we want to check "next to", because we need to be able to open/close from either side.
		// if it is a locked door, then we want to check if we're exactly on the closest tile to prevent speed-walking
		return lockedDoor == null
				? PathFinder.isNextTo(player.getFloor(), player.getTileId(), walkingTargetTileId)
				: player.getTileId() == walkingTargetTileId;
	}
	
	@Override
	protected void walkToTarget(Request request, Player player, ResponseMaps responseMaps) {
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), walkingTargetTileId, true));
	}

	@Override
	protected void doAction(Request request, Player player, ResponseMaps responseMaps) {
		final int newPlayerTileId = throughTileId == walkingTargetTileId ? tileId : throughTileId;
		if (lockedDoor != null) {
			// our destination tile is the tile that isn't closest, i.e. the one on the other side of the wall
			String failedRequirementReason = LockedDoorManager.playerMeetsDoorRequirements(player, lockedDoor);
			if (failedRequirementReason.isEmpty()) { // empty reason means the player meets the requirements
				if (LockedDoorManager.openLockedDoor(player.getFloor(), tileId)) // if it's already open then keep it open; just reset the timer
					responseMaps.addLocalResponse(player.getFloor(), tileId, this);
				// move the player to the other side				
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
				player.faceDirection(newPlayerTileId, responseMaps);
				
				setRecoAndResponseText(0, failedRequirementReason);
				responseMaps.addClientOnlyResponse(player, this);
			}
		} else {
			player.faceDirection(newPlayerTileId, responseMaps);
			
			DoorDao.toggleDoor(player.getFloor(), tileId);
			responseMaps.addLocalResponse(player.getFloor(), tileId, this);
		}
	}
}
