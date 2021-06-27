package main.responses;

import main.database.dao.ItemDao;
import main.database.dao.PickableDao;
import main.database.dao.PlayerStorageDao;
import main.database.dto.PickableDto;
import main.processing.DepletionManager;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.PickRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.StorageTypes;

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
			PickableDto pickable = PickableDao.getPickableByTileId(request.getTileId());
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
			
			setRecoAndResponseText(1, String.format("you pick some %s.", ItemDao.getNameFromId(pickable.getItemId())));
			responseMaps.addClientOnlyResponse(player, this);
			
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, freeSlotId, pickable.getItemId(), 1, ItemDao.getMaxCharges(pickable.getItemId()));
			new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
			
			DepletionManager.addDepletedScenery(DepletionManager.DepletionType.flower, player.getFloor(), request.getTileId(), pickable.getRespawnTicks(), responseMaps);
		}
	}

}