package main.responses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

public class BankDepositResponse extends Response {	
	private HashMap<Integer, InventoryItemDto> items;
	
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
		
		InventoryItemDto itemDto = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), request.getSlot());
		if (itemDto == null) {
			// cannot deposit this item (because it doesn't exist).
			return;
		}
		
		boolean bankContainsThisItem = false;
		
		ArrayList<Integer> bankItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.BANK.getValue());
		if (ItemDao.itemHasAttribute(itemDto.getItemId(), ItemAttributes.CHARGED)) {
			// because charged items stack only on those items with the same amount of charges, we need to run through
			// each of the items to see if there is an item with the same itemId and charges.
			HashMap<Integer, InventoryItemDto> bankItems = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.BANK.getValue());
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
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), request.getSlot(), 0, 1, 0);
			} else {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY.getValue(), request.getSlot(), -actualCount);
			}
		} else {
			
			ArrayList<Integer> slotsToDeposit = new ArrayList<>();
			slotsToDeposit.add(request.getSlot());
			ArrayList<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
			
			if (ItemDao.itemHasAttribute(itemDto.getItemId(), ItemAttributes.CHARGED)) {
				// num items in inventory with the same charge
				HashMap<Integer, InventoryItemDto> invItems = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
				for (int i = 0; i < invItemIds.size(); ++i) {
					if (i == request.getSlot())
						continue;
					
					if (invItemIds.get(i) == itemDto.getItemId()) {
						if (invItems.get(i).getCharges() == itemDto.getCharges())
							slotsToDeposit.add(i);
					}
					
					if (slotsToDeposit.size() >= actualCount)
						break;
				}
				actualCount = slotsToDeposit.size();
				
			} else {
				// non-charged non-stackable stuff
				for (int i = 0; i < invItemIds.size(); ++i) {
					if (i == request.getSlot())
						continue;
					
					if (invItemIds.get(i) == itemDto.getItemId()) {
						slotsToDeposit.add(i);
					}
					
					if (slotsToDeposit.size() >= actualCount)
						break;
				}
				actualCount = slotsToDeposit.size();
			}
			
			for (int slotToClear : slotsToDeposit)
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), slotToClear, 0, 1, 0);
		}
		
		PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.BANK.getValue(), itemDto.getItemId(), actualCount, itemDto.getCharges());
		
		// send an inv update to the player
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		
		// send a bank update to the player
		items = PlayerStorageDao.getStorageDtoMapByPlayerIdExcludingEmpty(player.getId(), StorageTypes.BANK.getValue());
		responseMaps.addClientOnlyResponse(player, this);
	}

}