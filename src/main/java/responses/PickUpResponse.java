package responses;

import database.dao.PetDao;
import database.dao.PlayerStorageDao;
import processing.PathFinder;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import processing.managers.HousePetsManager;
import processing.managers.HousingManager;
import processing.managers.LocationManager;
import requests.PickUpRequest;
import requests.Request;
import types.StorageTypes;

public class PickUpResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof PickUpRequest))
			return;
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		PickUpRequest request = (PickUpRequest)req;
		final NPC pet = LocationManager.getPetByFloorAndInstanceId(player.getFloor(), request.getObjectId());
		if (pet == null)
			return;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), pet.getTileId())) {
			player.setState(PlayerState.chasing);
			player.setSavedRequest(req);
			player.setTarget(pet);
		} else {
			// we're next to the pet
			player.faceDirection(pet.getTileId(), responseMaps);
			
			// following pets have the player's id, whereas house pets have the tileId as the instanceId
			int petHouseId = HousingManager.getHouseIdFromFloorAndTileId(player.getFloor(), pet.getInstanceId());
			
			if (!(request.getObjectId() == player.getId() || petHouseId == player.getHouseId())) {
				setRecoAndResponseText(0, "that doesn't belong to you.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			if (PlayerStorageDao.getFreeSlotByPlayerId(player.getId()) == -1) {
				setRecoAndResponseText(0, "you don't have enough inventory space.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, PetDao.getItemIdFromNpcId(pet.getDto().getId()), 1, 0);
			
			if (petHouseId > 0) {
				HousePetsManager.removePet(pet);
			} else {
				PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(player.getId(), StorageTypes.PET);
				player.setPet(null);
			}
			
			LocationManager.removePetIfExists(pet);
			
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
		}
	}

}
