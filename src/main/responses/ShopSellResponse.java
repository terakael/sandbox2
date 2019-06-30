package main.responses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import main.database.EquipmentDao;
import main.database.InventoryItemDto;
import main.database.ItemDao;
import main.database.ItemDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.processing.ShopManager;
import main.processing.Store;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.ShopSellRequest;
import main.types.ItemAttributes;
import main.types.Items;
import main.types.StorageTypes;

public class ShopSellResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ShopSellRequest))
			return;
		
		ShopSellRequest request = (ShopSellRequest)req;
		
		ArrayList<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
		if (!invItemIds.contains(request.getObjectId()))
			return; // player doesn't have it.
		
		ItemDto item = ItemDao.getItem(request.getObjectId());
		
		Store shop = ShopManager.getShopByShopId(player.getShopId());
		if (shop.isFull() && !shop.hasStock(item.getId())) {
			setRecoAndResponseText(0, "the shop has no room for your wares.");
			return;
		}
		
		if (item.getId() == Items.COINS.getValue()) {
			// trying to sell your coins lmfao?
			setRecoAndResponseText(0, "just what do you expect in return for selling your coins?  more coins?");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// you can't sell untradeable or unique items
		if (!ItemDao.itemHasAttribute(item.getId(), ItemAttributes.TRADEABLE) ||
				ItemDao.itemHasAttribute(item.getId(), ItemAttributes.UNIQUE) ||
				!shop.buysItem(item.getId())) {
			setRecoAndResponseText(0, "you can't sell that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// item passed all the checks, they can sell it
		int sellPrice = (int)((double)item.getPrice() * 0.8);// sell price is always 80% of buy price
		int numCoins = 0;
		if (ItemDao.itemHasAttribute(item.getId(), ItemAttributes.STACKABLE)) {
			// if the inventory is full and we don't have coins, but we're selling the whole stack
			// then we can replace the stack with the coins.
			InventoryItemDto invItem = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), invItemIds.indexOf(item.getId()));
			int sellCount = Math.min(invItem.getCount(), request.getAmount());
			if (sellCount < invItem.getCount() 
					&& PlayerStorageDao.getFreeSlotByPlayerId(player.getId()) == -1 
					&& !invItemIds.contains(Items.COINS.getValue())) {
				// full inventory with no coins, and we aren't selling the rest of the stack
				setRecoAndResponseText(0, "you don't have enough inventory space to accept your coins.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// we have room to accept the coins
			numCoins = sellCount * sellPrice;
			PlayerStorageDao.setCountOnInventorySlot(player.getId(), invItemIds.indexOf(item.getId()), invItem.getCount() - sellCount);
			shop.addItem(item.getId(), sellCount);
					
		} else {
			
			// exclude equipped items from the sale
			HashSet<Integer> equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getId());
			for (Integer slot : equippedSlots) {
				if (invItemIds.get(slot) == item.getId()) {
					invItemIds.set(slot, 0);// exclude the equipped item from the list
				}
			}
			
			int sellCount = Math.min(Collections.frequency(invItemIds, item.getId()), request.getAmount());
			if (sellCount > 0) {
				shop.addItem(item.getId(), sellCount);
				numCoins = sellCount * sellPrice;
				
				for (int i = 0; i < invItemIds.size() && sellCount > 0; ++i) {
					if (invItemIds.get(i) == item.getId()) {				
						PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), i, 0, 1);
						--sellCount;
					}
				}
			}
		}
		
		// items were sold, now give the player the coins
		if (invItemIds.contains(Items.COINS.getValue()))
			PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY.getValue(), invItemIds.indexOf(Items.COINS.getValue()), numCoins);
		else
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), Items.COINS.getValue(), numCoins);
//			PlayerStorageDao.addItemByPlayerIdItemId(player.getId(), Items.COINS.getValue(), numCoins);
		
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
	}
	
}
