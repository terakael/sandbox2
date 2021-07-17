package main.responses;

import java.util.ArrayList;
import java.util.List;

import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dto.InventoryItemDto;
import main.processing.PathFinder;
import main.processing.attackable.Player;
import main.processing.attackable.Player.PlayerState;
import main.processing.managers.DepletionManager;
import main.requests.OpenRequest;
import main.requests.Request;
import main.types.Items;
import main.types.StorageTypes;
import main.utils.RandomUtil;

public class OpenShadowChestResponse extends Response {
	private static final int SHADOW_KEY_ID = 353;
	
	private static List<Integer> shadowItemIds = List.<Integer>of(354, 355, 356, 357);
	private static List<List<InventoryItemDto>> dropTable = new ArrayList<>();
	
	static {
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.COINS.getValue(), 0, 6213, 0)
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.CABBAGE.getValue(), 0, 1, 0)
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.POISON_4.getValue(), 0, 1, 0),
				new InventoryItemDto(Items.POISON_4.getValue(), 0, 1, 0)
			));
			
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.MAGIC_PICKAXE.getValue(), 0, 1, ItemDao.getMaxCharges(Items.MAGIC_PICKAXE.getValue())),
				new InventoryItemDto(Items.TORNADO_RUNE.getValue(), 0, 12, 0)
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.FIRE_TORNADO_RUNE.getValue(), 0, 4, 0),
				new InventoryItemDto(351, 0, 1, 0) // gold chips
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.DISEASE_RUNE.getValue(), 0, 15, 0),
				new InventoryItemDto(320, 0, 1, 0), // wolf bones
				new InventoryItemDto(320, 0, 1, 0), // wolf bones
				new InventoryItemDto(320, 0, 1, 0), // wolf bones
				new InventoryItemDto(320, 0, 1, 0) // wolf bones
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(Items.TYROTOWN_TELEPORT_RUNE.getValue(), 0, 25, 0),
				new InventoryItemDto(Items.DECAY_RUNE.getValue(), 0, 3, 0)
			));
		
		dropTable.add(List.<InventoryItemDto>of(
				new InventoryItemDto(351, 0, 4, 0) // gold chips
			));
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		OpenRequest request = (OpenRequest)req;
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.faceDirection(request.getTileId(), responseMaps);
			
			if (DepletionManager.isDepleted(DepletionManager.DepletionType.chest, player.getFloor(), request.getTileId())) {
				return;
			}
			
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			int shadowKeyIndex = invItemIds.indexOf(SHADOW_KEY_ID);
			if (shadowKeyIndex == -1) {
				setRecoAndResponseText(0, "the chest is locked.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			DepletionManager.addDepletedScenery(DepletionManager.DepletionType.chest, player.getFloor(), request.getTileId(), 4, responseMaps);
			
			setRecoAndResponseText(1, "you unlock the chest to find treasure inside!");
			responseMaps.addClientOnlyResponse(player, this);
			
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, invItemIds.indexOf(SHADOW_KEY_ID), 0, 0, 0);
			
			// 10% chance of getting armour roll
			if (RandomUtil.chance(10)) {
				int shadowItemId = shadowItemIds.get(RandomUtil.getRandom(0, shadowItemIds.size()));
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, shadowItemId, 1, ItemDao.getMaxCharges(shadowItemId));
			} else {
				// one of the other treasures
				dropTable.get(RandomUtil.getRandom(0, dropTable.size())).forEach(item -> {
					PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, item.getItemId(), item.getCount(), item.getCharges());
				});
			}
			
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
			
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
					new ActionBubbleResponse(player, ItemDao.getItem(SHADOW_KEY_ID)));
		}
	}

}
