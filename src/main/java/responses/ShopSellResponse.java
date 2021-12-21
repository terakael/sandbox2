package responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import database.dao.EquipmentDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import database.dto.ItemDto;
import database.dto.ShopItemDto;
import processing.attackable.Player;
import processing.managers.ShopManager;
import processing.stores.Store;
import requests.Request;
import requests.RequestFactory;
import requests.ShopSellRequest;
import types.ItemAttributes;
import types.Items;
import types.StorageTypes;

public class ShopSellResponse extends Response {
	// if the client tries to send a different amount, then fail.  These are the only allowed amounts.
	private static Set<Integer> allowedRequestAmounts = new HashSet<>(Arrays.asList(1, 5, 10, 50));

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ShopSellRequest))
			return;
		
		final ShopSellRequest request = (ShopSellRequest)req;
		
		int requestAmount = request.getAmount();
		if (!allowedRequestAmounts.contains(requestAmount))
			return;
		
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
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
			setRecoAndResponseText(0, "you can't sell that here.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}

		// item passed all the checks, they can sell it
		int numCoins = 0;
		if (ItemDao.itemHasAttribute(item.getId(), ItemAttributes.STACKABLE)) {
			// if the inventory is full and we don't have coins, but we're selling the whole stack
			// then we can replace the stack with the coins.
			InventoryItemDto invItem = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, invItemIds.indexOf(item.getId()));
			int sellCount = Math.min(invItem.getCount(), requestAmount);
			if (sellCount < invItem.getCount() 
					&& PlayerStorageDao.getFreeSlotByPlayerId(player.getId()) == -1 
					&& !invItemIds.contains(Items.COINS.getValue())) {
				// full inventory with no coins, and we aren't selling the rest of the stack
				setRecoAndResponseText(0, "you don't have enough inventory space to accept your coins.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			int[] out = {0, 0};
			ShopItemDto shopItem = shop.getStockByItemId(item.getId());
			if (shopItem == null)
				shopItem = new ShopItemDto(item.getId(), 0, 0, item.getPrice(), 100);
			calculatePlayerTotalSalePrice(sellCount, shop, shopItem, out);
			
			// we have room to accept the coins
			numCoins = out[0];
			PlayerStorageDao.setCountOnSlot(player.getId(), StorageTypes.INVENTORY, invItemIds.indexOf(item.getId()), invItem.getCount() - out[1]);
			shop.addItem(item.getId(), out[1]);
					
		} else {
			
			// exclude equipped items from the sale
			Set<Integer> equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getId());
			for (Integer slot : equippedSlots) {
				if (invItemIds.get(slot) == item.getId()) {
					invItemIds.set(slot, 0);// exclude the equipped item from the list
				}
			}
			
			// exclude partially charged items from the sale
			for (int i = 0; i < invItemIds.size(); ++i) {
				if (ItemDao.itemHasAttribute(invItemIds.get(i), ItemAttributes.CHARGED)) {
					InventoryItemDto invItem = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, i);
					if (invItem.getCharges() != ItemDao.getMaxCharges(invItem.getItemId())) {
						// not fully charged, so we won't sell it
						invItemIds.set(i, 0);
					}
				}
			}
			
			int sellCount = Math.min(Collections.frequency(invItemIds, item.getId()), requestAmount);
			
			int[] out = {0, 0};
			ShopItemDto shopItem = shop.getStockByItemId(item.getId());
			if (shopItem == null)
				shopItem = new ShopItemDto(item.getId(), 0, 0, item.getPrice(), 100);
			calculatePlayerTotalSalePrice(sellCount, shop, shopItem, out);
			sellCount = out[1];
			
			if (sellCount > 0) {
				shop.addItem(item.getId(), sellCount);
				numCoins = out[0];
				
				for (int i = 0; i < invItemIds.size() && sellCount > 0; ++i) {
					if (invItemIds.get(i) == item.getId()) {				
						PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, i, 0, 1, 0);
						--sellCount;
					}
				}
			} else {
				// no items were sold - is it because they tried to sell partially charged stuff?
				if (ItemDao.itemHasAttribute(item.getId(), ItemAttributes.CHARGED)) {
					setRecoAndResponseText(0, "shops won't accept partially charged items.");
					responseMaps.addClientOnlyResponse(player, this);
				}
			}
		}
		
		if (numCoins > 0) {
			// items were sold, now give the player the coins
			if (invItemIds.contains(Items.COINS.getValue()))
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY, invItemIds.indexOf(Items.COINS.getValue()), numCoins);
			else
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, Items.COINS.getValue(), numCoins, 0);
	//			PlayerStorageDao.addItemByPlayerIdItemId(player.getId(), Items.COINS.getValue(), numCoins);
		}
		
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
	}
	
	private void calculatePlayerTotalSalePrice(int requestAmount, Store shop, ShopItemDto item, int[] output) {
		// pretty dirty hack for the output but basically i need two things returned:
		// [0] how many coins i'll gain
		// [1] how many items i'll sell
		
		for (int i = 0; i < requestAmount; ++i, ++output[1]) {
			int currentPrice = shop.getShopBuyPriceAt(item, item.getCurrentStock() + i); 
			output[0] += currentPrice;
		}
	}
	
}
