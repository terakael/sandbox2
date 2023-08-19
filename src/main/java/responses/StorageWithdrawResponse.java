package responses;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import requests.WithdrawRequest;
import types.ItemAttributes;
import types.Storage;
import types.StorageTypes;

public abstract class StorageWithdrawResponse extends Response {
	private List<InventoryItemDto> items;
	
	public StorageWithdrawResponse() {
		setAction("withdraw");
		setCombatInterrupt(false);
	}
	
	protected void withdraw(Player player, Storage storage, WithdrawRequest request, ResponseMaps responseMaps) {
		List<InventoryItemDto> storageItems = storage.getItems();
		InventoryItemDto targetItem = storageItems.get(request.getSlot());
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
			storage.addStackable(targetItem, -actualCount);
		} else {
			if (freeInventorySlots == 0) {
				setRecoAndResponseText(0, "your inventory is full.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// the "actual count" is the smallest value between the request count, how many items there are in the chest, and how many free inventory spaces.
			actualCount = Math.min(actualCount, (int)storageItems.stream().filter(item -> item.getItemId() == targetItem.getItemId()).count());
			actualCount = Math.min(actualCount, freeInventorySlots);
			
			// remove the first one from the correct slot
			storage.remove(request.getSlot());
			storageItems.set(request.getSlot(), new InventoryItemDto(0, request.getSlot(), 0, 0)); // exclude the item from the temporary variable
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, targetItem.getItemId(), 1, targetItem.getCharges());
			
			List<Integer> matchingItemSlots = storageItems.stream()
					.filter(e -> e.getItemId() == targetItem.getItemId() && e.getCharges() == targetItem.getCharges())
					.map(InventoryItemDto::getSlot)
					.collect(Collectors.toList());
			
			// remove the rest one by one
			for (int i = 0; i < actualCount - 1; ++i) {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, targetItem.getItemId(), 1, targetItem.getCharges());
				storage.remove(matchingItemSlots.get(i));
			}
		}
		
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		items = storage.getItems();
		responseMaps.addClientOnlyResponse(player, this);
	}
}
