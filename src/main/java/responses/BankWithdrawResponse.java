package responses;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.InventoryItemDto;
import processing.PathFinder;
import processing.attackable.Player;
import requests.Request;
import requests.RequestFactory;
import requests.WithdrawRequest;
import types.ItemAttributes;
import types.StorageTypes;

@SuppressWarnings("unused")
public class BankWithdrawResponse extends Response {
	private Map<Integer, InventoryItemDto> items;
	
	public BankWithdrawResponse() {
		setAction("withdraw");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof WithdrawRequest))
			return;
		
		WithdrawRequest request = (WithdrawRequest)req;
		
		InventoryItemDto bankItemDto = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.BANK, request.getSlot());
		if (bankItemDto == null || bankItemDto.getItemId() == 0) {
			// cannot withdraw this item (because it doesn't exist).
			return;
		}
		
		List<Integer> inventoryItems = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		
		if (!inventoryItems.contains(0)) {
			if (!ItemDao.itemHasAttribute(bankItemDto.getItemId(), ItemAttributes.STACKABLE) || !inventoryItems.contains(bankItemDto.getItemId())) {
				setRecoAndResponseText(0, "inventory is full.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
		}

		int actualCount = request.getAmount() == -1 ? Integer.MAX_VALUE : request.getAmount();
		if (ItemDao.itemHasAttribute(bankItemDto.getItemId(), ItemAttributes.STACKABLE)) {
			actualCount = Math.min(bankItemDto.getCount(), actualCount);
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, bankItemDto.getItemId(), actualCount, bankItemDto.getCharges());
		} else {
			int withdrawnCount = 0;
			// the "actual count" is the smallest value between the request count, how many items there are in the bank, and how many inventory spaces.
			actualCount = Math.min(actualCount, bankItemDto.getCount());
			actualCount = Math.min(actualCount, Collections.frequency(inventoryItems, 0));
			
			for (int slot = 0; slot < inventoryItems.size(); ++slot) {
				if (inventoryItems.get(slot) != 0)
					continue;
				
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, bankItemDto.getItemId(), 1, bankItemDto.getCharges());
				
				if (++withdrawnCount >= actualCount) {
					break;
				}
			}
		}
		
		if (actualCount >= bankItemDto.getCount()) {
			// the whole stack is going into the bank, so clear the inventory space
//			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.BANK, request.getSlot(), 0, 1, 0);
			
			Map<Integer, InventoryItemDto> bankList = PlayerStorageDao.getStorageDtoMapByPlayerIdExcludingEmpty(player.getId(), StorageTypes.BANK);
			PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(player.getId(), StorageTypes.BANK);
			
			int newslot = 0;
			bankList.remove(request.getSlot());
			for (InventoryItemDto dto : bankList.values()) {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.BANK, newslot++, dto.getItemId(), dto.getCount(), dto.getCharges());
			}
			
			
			
			// TODO shuffle all the bank slots back by one to fill the gap
			// TODO should the shuffle happen in real-time or when the player closes their bank?
		} else {
			PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.BANK, request.getSlot(), -actualCount);
		}
		
		
		// send an inv update to the player
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		
		// send a bank update to the player
		items = PlayerStorageDao.getStorageDtoMapByPlayerIdExcludingEmpty(player.getId(), StorageTypes.BANK);
		responseMaps.addClientOnlyResponse(player, this);
	}

}
