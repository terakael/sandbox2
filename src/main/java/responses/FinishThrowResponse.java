package responses;

import java.util.ArrayList;
import java.util.List;

import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.InventoryItemDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.managers.LocationManager;
import processing.managers.UndeadArmyManager;
import requests.Request;
import types.Items;
import types.StorageTypes;
import utils.RandomUtil;
import utils.Utils;

public class FinishThrowResponse extends Response {
	
	private static final int throwRange = 2;
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		final List<Integer> playerInvIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		final int slot = playerInvIds.indexOf(Items.ZOMBIE_SEEDS.getValue());
		if (slot == -1)
			return; // player doesn't have seeds, bail silently.
		
		int chosenTileId = -1;
		List<Integer> localTileIds = new ArrayList<>(Utils.getLocalTiles(player.getTileId(), throwRange));
		do {
			int randomIndex = RandomUtil.getRandom(0, localTileIds.size());
			chosenTileId = localTileIds.get(randomIndex);
			
			if (tileIsValid(player, chosenTileId))
				break;
			
			localTileIds.remove(randomIndex);
		} while (!localTileIds.isEmpty());
		
		if (!localTileIds.isEmpty()) {
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("...and a zombie sprouts.", "white"));
			UndeadArmyManager.addPlayerGrownZombie(player, player.getFloor(), chosenTileId);
		} else {
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("...and the seed just sits there.", "white"));
		}
		
		final InventoryItemDto seeds = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot);
		PlayerStorageDao.setCountOnSlot(player.getId(), StorageTypes.INVENTORY, slot, seeds.getCount() - 1);
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
	}
	
	private boolean tileIsValid(Player player, int checkTileId) {
		if (!PathFinder.tileIsWalkable(player.getFloor(), checkTileId)) // water, blank tiles etc
			return false;
		
		if (SceneryDao.getSceneryIdByTileId(player.getFloor(), checkTileId) != -1)
			return false;
		
		if (LocationManager.getNpcNearPlayerByInstanceId(player, checkTileId) != null)
			return false;
		
		if (!PathFinder.lineOfSightIsClear(player.getFloor(), player.getTileId(), checkTileId, throwRange * 2))
			return false;
		
		return true;
	}

}
