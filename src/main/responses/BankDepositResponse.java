package main.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.database.EquipmentDao;
import main.database.InventoryItemDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.BankDepositRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.ItemAttributes;
import main.types.StorageTypes;

@SuppressWarnings("unused")
public class BankDepositResponse extends Response {	
	private Map<Integer, InventoryItemDto> items;
	
	public BankDepositResponse() {
		setAction("deposit");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof BankDepositRequest))
			return;
		
		BankDepositRequest request = (BankDepositRequest)req;
		
		if (EquipmentDao.isSlotEquipped(player.getId(), request.getSlot())) {
			setRecoAndResponseText(0, "you need to unequip it first.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		InventoryItemDto itemDto = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot());
		if (itemDto == null || itemDto.getItemId() == 0) {
			// cannot deposit this item (because it doesn't exist).
			return;
		}
		
		boolean bankContainsThisItem = false;
		
		List<Integer> bankItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.BANK);
		if (ItemDao.itemHasAttribute(itemDto.getItemId(), ItemAttributes.CHARGED)) {
			// because charged items stack only on those items with the same amount of charges, we need to run through
			// each of the items to see if there is an item with the same itemId and charges.
			Map<Integer, InventoryItemDto> bankItems = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.BANK);
			for (Map.Entry<Integer, InventoryItemDto> entry : bankItems.entrySet()) {
				if (entry.getValue().getItemId() == itemDto.getItemId() && entry.getValue().getCharges() == itemDto.getCharges()) {
					bankContainsThisItem = true;
					break;
				}
			}
		} else {
			bankContainsThisItem = bankItemIds.contains(itemDto.getItemId());
		}
		
		if (!bankItemIds.contains(0) && !bankContainsThisItem) {
			setRecoAndResponseText(0, "bank is full.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// if the count is -1 it means deposit all.
		int actualCount = request.getAmount() == -1 ? Integer.MAX_VALUE : request.getAmount();
		if (ItemDao.itemHasAttribute(itemDto.getItemId(), ItemAttributes.STACKABLE)) {
			actualCount = Math.min(itemDto.getCount(), actualCount);
			if (actualCount >= itemDto.getCount()) {
				// the whole stack is going into the bank, so clear the inventory space
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), 0, 1, 0);
			} else {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), -actualCount);
			}
		} else {
			
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
			
			for (int slotToClear : slotsToDeposit)
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slotToClear, 0, 1, 0);
		}
		
		PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.BANK, itemDto.getItemId(), actualCount, itemDto.getCharges());
		
		// send an inv update to the player
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		
		// send a bank update to the player
		items = PlayerStorageDao.getStorageDtoMapByPlayerIdExcludingEmpty(player.getId(), StorageTypes.BANK);
		responseMaps.addClientOnlyResponse(player, this);
	}

}