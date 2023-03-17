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
	
//	@Override
//	public void process(Request req, Player player, ResponseMaps responseMaps) {
//		OpenRequest request = (OpenRequest)req;
//		tileId = request.getTileId();
//		
//		// does the tile a door on it?
//		DoorDto door = DoorDao.getDoorDtoByTileId(player.getFloor(), tileId);
//		if (door == null) {
//			// this can technically happen when the player clicks a ladder, then right-clicks a door
//			// then finally selects "open" after they have switched rooms.  Do not do anything in this case.
//			return;
//		}
//		
//		final int impassable = DoorDao.getDoorImpassableByTileId(player.getFloor(), tileId);
//		int throughTileId = PathFinder.calculateThroughTileId(tileId, impassable);
//		if (throughTileId == -1)
//			return; // multiple sides means we can't deduce where to walk through
//		
//		// we want to start by walking to the closest tile to us (i.e. the side of the wall we're on)
//		int closestTileId = PathFinder.getCloserTile(player.getTileId(), tileId, throughTileId);
//		
//		// if we're around a corner from a door then the inside of a closed door could be the closest tile.
//		// we need to check if we can actually move to the tile.
//		if (closestTileId != player.getTileId() && PathFinder.findPath(player.getFloor(), player.getTileId(), closestTileId, true).isEmpty())
//			closestTileId = closestTileId == tileId ? throughTileId : tileId;
//		
//		final LockedDoorDto lockedDoor = LockedDoorManager.getLockedDoor(player.getFloor(), tileId);
//		
//		// if it's not a locked door, we want to check "next to", because we need to be able to open/close from either side.
//		// if it is a locked door, then we want to check if we're exactly on the closest tile to prevent speed-walking
//		final boolean closeEnoughToOpen = lockedDoor == null
//				? PathFinder.isNextTo(player.getFloor(), player.getTileId(), closestTileId)
//				: player.getTileId() == closestTileId;
//		
//		if (!closeEnoughToOpen) {
//			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), closestTileId, true));
//			player.setState(PlayerState.walking);
//			player.setSavedRequest(req);
//			return;
//		} else {			
//			player.faceDirection(tileId, responseMaps);
//			if (lockedDoor != null) {
//				String failedRequirementReason = LockedDoorManager.playerMeetsDoorRequirements(player, lockedDoor);
//				if (failedRequirementReason.isEmpty()) { // empty reason means the player meets the requirements
//					if (LockedDoorManager.openLockedDoor(player.getFloor(), tileId)) // if it's already open then keep it open; just reset the timer
//						responseMaps.addLocalResponse(player.getFloor(), tileId, this);
//					// move the player to the other side
//					// our destination tile is the tile that isn't closest, i.e. the one on the other side of the wall
//					final int newPlayerTileId = throughTileId == closestTileId ? tileId : throughTileId;
//					
//					PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
//					playerUpdate.setId(player.getId());
//					playerUpdate.setTileId(newPlayerTileId);
//					player.setTileId(newPlayerTileId);
//					responseMaps.addLocalResponse(player.getFloor(), tileId, playerUpdate);
//					
//					// sometimes a key should be used only once, and removed from inventory on use.
//					if (lockedDoor.isDestroyOnUse()) {
//						List<Integer> playerInvIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
//						int slot = playerInvIds.indexOf(lockedDoor.getUnlockItemId());
//						if (slot != -1) {
//							PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, 0, 0, 0);
//							InventoryUpdateResponse.sendUpdate(player, responseMaps);
//						}
//					}
//				} else {
//					setRecoAndResponseText(0, failedRequirementReason);
//					responseMaps.addClientOnlyResponse(player, this);
//				}
//			} else {
//				DoorDao.toggleDoor(player.getFloor(), tileId);
//				responseMaps.addLocalResponse(player.getFloor(), tileId, this);
//			}
//		}
//	}
	
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
		if (lockedDoor != null) {
			String failedRequirementReason = LockedDoorManager.playerMeetsDoorRequirements(player, lockedDoor);
			if (failedRequirementReason.isEmpty()) { // empty reason means the player meets the requirements
				if (LockedDoorManager.openLockedDoor(player.getFloor(), tileId)) // if it's already open then keep it open; just reset the timer
					responseMaps.addLocalResponse(player.getFloor(), tileId, this);
				// move the player to the other side
				// our destination tile is the tile that isn't closest, i.e. the one on the other side of the wall
				final int newPlayerTileId = throughTileId == walkingTargetTileId ? tileId : throughTileId;
				
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
