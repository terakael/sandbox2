package main.responses;

import main.database.dao.ConstructableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.processing.PathFinder;
import main.processing.attackable.Player;
import main.processing.attackable.Player.PlayerState;
import main.processing.managers.FightManager;
import main.requests.AssembleRequest;
import main.requests.Request;
import main.types.StorageTypes;

public class AssembleResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		final int slot = ((AssembleRequest)req).getSlot();
		final int itemId = PlayerStorageDao.getItemIdInSlot(player.getId(), StorageTypes.INVENTORY, slot);
		if (ConstructableDao.getConstructableByFlatpackItemId(itemId) == null)
			return;
		
		int existingSceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), req.getTileId());
		if (existingSceneryId != -1) {
			setRecoAndResponseText(0, "you can't build that here.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		int newTileId = ConstructionResponse.findDestinationTileId(player.getFloor(), player.getTileId());
		if (newTileId == player.getTileId()) {
			// somehow no tiles are free?  are you inside a tree or something?
			setRecoAndResponseText(0, "you can't build that here.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// move the player to the side so the constructable doesn't appear on top of the player
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(player.getId());
		playerUpdate.setTileId(newTileId);
		playerUpdate.setFaceDirection(PathFinder.getDirection(newTileId, player.getTileId()));
		responseMaps.addLocalResponse(player.getFloor(), newTileId, playerUpdate);
		player.setTileId(newTileId);
		
		player.setState(PlayerState.assembling);
		player.setSavedRequest(req);
		player.setTickCounter(3);

		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), new ActionBubbleResponse(player, ItemDao.getItem(itemId)));
	}

}
