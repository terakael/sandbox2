package main.responses;

import java.util.ArrayList;
import java.util.List;

import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.database.dto.InventoryItemDto;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.LootRequest;
import main.requests.Request;
import main.types.Items;
import main.types.StorageTypes;
import main.utils.RandomUtil;

public class LootResponse extends Response {
	
	private static List<Integer> necroRobeItemIds = List.<Integer>of(394, 395, 396);
	private static List<List<InventoryItemDto>> dropTable = new ArrayList<>();
	
	static {
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.COINS.getValue(), 0, 5880, 0)
			));
			
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.TORNADO_RUNE.getValue(), 0, 100, 0)
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.FIRE_TORNADO_RUNE.getValue(), 0, 100, 0)
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.DISEASE_RUNE.getValue(), 0, 100, 0)
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.DECAY_RUNE.getValue(), 0, 100, 0)
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.CRUMBLE_UNDEAD_RUNE.getValue(), 0, 100, 0)
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.ZOMBIE_SEEDS.getValue(), 0, 5, 0)
			));
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		if (!(req instanceof LootRequest))
			return;
		
		LootRequest request = (LootRequest)req;
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {			
			player.faceDirection(request.getTileId(), responseMaps);
			
			int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
			if (sceneryId == 156) { // necrotic chest
				handleLoot(player, responseMaps);
				
				int graveyardEntranceFloor = 0;
				int graveyardEntranceTileId = 937916240;
				
				// send teleport explosions to both where the player teleported from, and where they're teleporting to
				// that way players on both sides of the teleport will see it
				responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), new TeleportExplosionResponse(player.getTileId()));
				responseMaps.addLocalResponse(graveyardEntranceFloor, graveyardEntranceTileId, new TeleportExplosionResponse(graveyardEntranceTileId));
				
				PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
				playerUpdate.setId(player.getId());
				playerUpdate.setTileId(graveyardEntranceTileId);
				playerUpdate.setSnapToTile(true);
				
				responseMaps.addClientOnlyResponse(player, playerUpdate);
				responseMaps.addLocalResponse(graveyardEntranceFloor, graveyardEntranceTileId, playerUpdate);
				
				player.setFloor(graveyardEntranceFloor, responseMaps);
				player.setTileId(graveyardEntranceTileId);
				
				player.clearPath();
			}
		}
	}
	
	private void handleLoot(Player player, ResponseMaps responseMaps) {
		// 10% chance of getting armour roll
		if (RandomUtil.chance(10)) {
			int necroRobeItemId = necroRobeItemIds.get(RandomUtil.getRandom(0, necroRobeItemIds.size()));
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, necroRobeItemId, 1, ItemDao.getMaxCharges(necroRobeItemId));
		} else {
			// one of the other treasures
			dropTable.get(RandomUtil.getRandom(0, dropTable.size())).forEach(item -> {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, item.getItemId(), item.getCount(), item.getCharges());
			});
		}
		
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
	}

}