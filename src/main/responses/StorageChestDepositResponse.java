package main.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.database.dao.EquipmentDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dto.InventoryItemDto;
import main.processing.attackable.Player;
import main.processing.managers.ConstructableManager;
import main.processing.scenery.constructable.Constructable;
import main.processing.scenery.constructable.StorageChest;
import main.requests.DepositRequest;
import main.requests.Request;
import main.types.ItemAttributes;
import main.types.StorageTypes;

@SuppressWarnings("unused")
public class StorageChestDepositResponse extends Response {
	private List<InventoryItemDto> items = null;
	
	public StorageChestDepositResponse() {
		setAction("deposit");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		DepositRequest request = (DepositRequest)req;
		Constructable constructable = ConstructableManager.getConstructableInstanceByTileId(player.getFloor(), request.getTileId());
		if (constructable == null)
			return;
		
		if (!(constructable instanceof StorageChest))
			return;
		
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
		
		StorageChest chest = (StorageChest)constructable;		
		int remainingEmptySlots = chest.getEmptySlotCount(player.getId());
		if (remainingEmptySlots == 0 && !(itemIsStackable && chest.contains(player.getId(), itemDto.getItemId()))) {
			setRecoAndResponseText(0, "the chest is full.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		int actualCount = request.getAmount() == -1 ? Integer.MAX_VALUE : request.getAmount();
		if (itemIsStackable) {
			actualCount = Math.min(itemDto.getCount(), actualCount);
			if (actualCount >= itemDto.getCount()) {
				// the whole stack is going into the bank, so clear the inventory space
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), 0, 1, 0);
			} else {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), -actualCount);
			}
			
			chest.addStackable(player.getId(), itemDto, actualCount);
		} else {
			
			actualCount = Math.min(actualCount, remainingEmptySlots);
			
			List<Integer> slotsToDeposit = new ArrayList<>();
			slotsToDeposit.add(request.getSlot());
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			
			// remove any equipped items from consideration
			Set<Integer> equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getId());
			for (Integer slot : equippedSlots) {
				if (invItemIds.get(slot) == itemDto.getItemId()) {
					invItemIds.set(slot, 0);// exclude the equipped item from the list
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
				chest.add(player.getId(), itemDto);
			}
		}
		
		items = chest.getItems(player.getId());
		
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
