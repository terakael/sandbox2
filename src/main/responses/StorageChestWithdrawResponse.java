package main.responses;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dto.InventoryItemDto;
import main.processing.ConstructableManager;
import main.processing.Player;
import main.requests.Request;
import main.requests.WithdrawRequest;
import main.scenery.constructable.Constructable;
import main.scenery.constructable.StorageChest;
import main.types.ItemAttributes;
import main.types.StorageTypes;

public class StorageChestWithdrawResponse extends Response {
	private List<InventoryItemDto> items;
	
	public StorageChestWithdrawResponse() {
		setAction("withdraw");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		WithdrawRequest request = (WithdrawRequest)req;
		
		final Constructable constructable = ConstructableManager.getConstructableInstanceByTileId(player.getFloor(), request.getTileId());
		if (constructable == null)
			return;
		
		if (!(constructable instanceof StorageChest))
			return;
		
		StorageChest chest = (StorageChest)constructable;		
		
		List<InventoryItemDto> chestItems = chest.getItems(player.getId());
		InventoryItemDto targetItem = chestItems.get(request.getSlot());
		if (targetItem.getItemId() == 0)
			return;
		
		List<Integer> inventoryItems = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		int freeInventorySlots = Collections.frequency(inventoryItems, 0);
		
		int actualCount = request.getAmount() == -1 ? Integer.MAX_VALUE : request.getAmount();
		if (ItemDao.itemHasAttribute(targetItem.getItemId(), ItemAttributes.STACKABLE)) {
			if (freeInventorySlots == 0 && !inventoryItems.contains(targetItem.getItemId())) {
				setRecoAndResponseText(0, "your inventory is full.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			actualCount = Math.min(targetItem.getCount(), actualCount);
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, targetItem.getItemId(), actualCount, targetItem.getCharges());
			chest.addStackable(player.getId(), targetItem, -actualCount);
		} else {
			if (freeInventorySlots == 0) {
				setRecoAndResponseText(0, "your inventory is full.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// the "actual count" is the smallest value between the request count, how many items there are in the chest, and how many free inventory spaces.
			actualCount = Math.min(actualCount, (int)chestItems.stream().filter(item -> item.getItemId() == targetItem.getItemId()).count());
			actualCount = Math.min(actualCount, freeInventorySlots);
			
			// remove the first one from the correct slot
			chest.remove(player.getId(), request.getSlot());
			chestItems.set(request.getSlot(), new InventoryItemDto(0, request.getSlot(), 0, 0)); // exclude the item from the temporary variable
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, targetItem.getItemId(), 1, targetItem.getCharges());
			
			List<Integer> matchingItemSlots = chestItems.stream()
					.filter(e -> e.getItemId() == targetItem.getItemId() && e.getCharges() == targetItem.getCharges())
					.map(InventoryItemDto::getSlot)
					.collect(Collectors.toList());
			
			// remove the rest one by one
			for (int i = 0; i < actualCount - 1; ++i) {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, targetItem.getItemId(), 1, targetItem.getCharges());
				chest.remove(player.getId(), matchingItemSlots.get(i));
			}
		}
		
		items = chest.getItems(player.getId());
		responseMaps.addClientOnlyResponse(player, this);
		
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
	}

}
