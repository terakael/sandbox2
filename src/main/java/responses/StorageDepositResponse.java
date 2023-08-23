package responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import database.dao.EquipmentDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import lombok.Setter;
import processing.attackable.Player;
import requests.DepositRequest;
import types.ItemAttributes;
import types.Storage;
import types.StorageTypes;

public abstract class StorageDepositResponse extends Response {
	@Setter private List<InventoryItemDto> items = null;
	
	public StorageDepositResponse() {
		setAction("deposit");
		setCombatInterrupt(false);
	}
	
	public void deposit(Player player, Storage storage, DepositRequest request, ResponseMaps responseMaps) {
		InventoryItemDto itemDto = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot());
		if (itemDto == null || itemDto.getItemId() == 0) {
			// cannot deposit this item (because it doesn't exist).
			return;
		}
		
		
		if (EquipmentDao.isSlotEquipped(player.getId(), request.getSlot())) {
			setRecoAndResponseText(0, "you need to unequip it first.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		final boolean itemIsStackable = ItemDao.itemHasAttribute(itemDto.getItemId(), ItemAttributes.STACKABLE);
		
		int actualCount = request.getAmount() == -1 ? Integer.MAX_VALUE : request.getAmount();
		
		int remainingEmptySlots = storage.getEmptySlotCount();
		if (remainingEmptySlots == 0 && !(itemIsStackable && storage.contains(itemDto.getItemId()))) {
			setRecoAndResponseText(0, "there's no more room.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (itemIsStackable) {
			actualCount = Math.min(itemDto.getCount(), actualCount);
			if (actualCount >= itemDto.getCount()) {
				// the whole stack is going into the bank, so clear the inventory space
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), 0, 1, 0);
			} else {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), -actualCount);
			}
			
			storage.addStackable(itemDto, actualCount);
		} else {
			actualCount = Math.min(actualCount, storage.getEmptySlotCount());
			
			List<Integer> slotsToDeposit = new ArrayList<>();
			slotsToDeposit.add(request.getSlot());
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			
			// remove any equipped items from consideration
			Set<Integer> equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getId());
			for (Integer equippedSlot : equippedSlots) {
				if (invItemIds.get(equippedSlot) == itemDto.getItemId()) {
					invItemIds.set(equippedSlot, 0);// exclude the equipped item from the list
				}
			}

			if (ItemDao.itemHasAttribute(itemDto.getItemId(), ItemAttributes.CHARGED)) {
				// num items in inventory with the same charge
				Map<Integer, InventoryItemDto> invItems = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.INVENTORY);
				
				
				for (int i = 0; i < invItemIds.size(); ++i) {
					if (i == request.getSlot())
						continue;
					
					if (slotsToDeposit.size() >= actualCount)
						break;
					
					if (invItemIds.get(i) == itemDto.getItemId()) {
						if (invItems.get(i).getCharges() == itemDto.getCharges())
							slotsToDeposit.add(i);
					}
				}
				actualCount = slotsToDeposit.size();
				
			} else {
				// non-charged non-stackable stuff
				for (int i = 0; i < invItemIds.size(); ++i) {
					if (i == request.getSlot())
						continue;
					
					if (slotsToDeposit.size() >= actualCount)
						break;
					
					if (invItemIds.get(i) == itemDto.getItemId()) {
						slotsToDeposit.add(i);
					}
				}
				actualCount = slotsToDeposit.size();
			}
			
			for (int slotToClear : slotsToDeposit) {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slotToClear, 0, 1, 0);
				
				if (storage.isAllItemsStackable()) {
					storage.addStackable(itemDto, itemDto.getCount());
				} else {
					storage.add(itemDto, itemDto.getCount());
				}
			}
		}
		
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		items = storage.getItems();
		responseMaps.addClientOnlyResponse(player, this);
	}
}
