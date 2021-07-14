package main.responses;

import java.util.ArrayList;
import java.util.List;

import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.database.dto.InventoryItemDto;
import main.processing.NPCManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.UndeadArmyManager;
import main.requests.Request;
import main.types.Items;
import main.types.StorageTypes;
import main.utils.RandomUtil;
import main.utils.Utils;

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
		if (!PathFinder.tileIsValid(player.getFloor(), checkTileId)) // water, blank tiles etc
			return false;
		
		if (SceneryDao.getSceneryIdByTileId(player.getFloor(), checkTileId) != -1)
			return false;
		
		if (NPCManager.get().getNpcByInstanceId(player.getFloor(), checkTileId) != null)
			return false;
		
		if (!PathFinder.lineOfSightIsClear(player.getFloor(), player.getTileId(), checkTileId, throwRange * 2))
			return false;
		
		return true;
	}

}
