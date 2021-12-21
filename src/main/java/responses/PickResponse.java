package responses;

import database.dao.ItemDao;
import database.dao.PickableDao;
import database.dao.PlayerStorageDao;
import database.dto.PickableDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.ArtisanManager;
import processing.managers.DepletionManager;
import processing.managers.FightManager;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.PickTaskUpdate;
import requests.PickRequest;
import requests.Request;
import requests.RequestFactory;
import types.StorageTypes;

public class PickResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof PickRequest))
			return;
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		PickRequest request = (PickRequest)req;
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.faceDirection(request.getTileId(), responseMaps);
			PickableDto pickable = PickableDao.getPickableByTileId(player.getFloor(), request.getTileId());
			if (pickable == null) {
				setRecoAndResponseText(0, "you can't pick that.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			if (DepletionManager.isDepleted(DepletionManager.DepletionType.flower, player.getFloor(), request.getTileId())) {
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
			
			setRecoAndResponseText(1, String.format("you pick some %s.", ItemDao.getNameFromId(pickable.getItemId(), false)));
			responseMaps.addClientOnlyResponse(player, this);
			
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, freeSlotId, pickable.getItemId(), 1, ItemDao.getMaxCharges(pickable.getItemId()));
			new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
			
			TybaltsTaskManager.check(player, new PickTaskUpdate(pickable.getItemId()), responseMaps);
			ArtisanManager.check(player, pickable.getItemId(), responseMaps);
			
			DepletionManager.addDepletedScenery(DepletionManager.DepletionType.flower, player.getFloor(), request.getTileId(), pickable.getRespawnTicks(), responseMaps);
		}
	}

}