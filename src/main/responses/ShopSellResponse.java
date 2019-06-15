package main.responses;

import java.util.ArrayList;
import java.util.Collections;

import main.database.InventoryItemDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.database.ShopDao;
import main.database.ShopDto;
import main.processing.Player;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.ShopSellRequest;
import main.types.ItemAttributes;
import main.types.Items;

public class ShopSellResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ShopSellRequest))
			return;
		
		ShopSellRequest request = (ShopSellRequest)req;
		
		ArrayList<Integer> invItemIds = PlayerStorageDao.getInventoryListByPlayerId(player.getId());
		if (!invItemIds.contains(request.getObjectId()))
			return; // player doesn't have it.
		
		ShopDto item = null;
		for (ShopDto dto : ShopDao.getShopStockById(player.getShopId())) {
			if (dto.getItemId() == request.getObjectId()) {
				item = dto;
				break;
			}
		}
		
		if (item == null) {
			setRecoAndResponseText(0, "you can't sell that here.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (item.getItemId() == Items.COINS.getValue()) {
			// trying to sell your coins lmfao?
			setRecoAndResponseText(0, "just what do you expect in return for selling your coins?  more coins?");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// you can't sell untradeable or unique items
		if (!ItemDao.itemHasAttribute(item.getItemId(), ItemAttributes.TRADEABLE) ||
				ItemDao.itemHasAttribute(item.getItemId(), ItemAttributes.UNIQUE)) {
			setRecoAndResponseText(0, "you can't sell that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// item passed all the checks, they can sell it
		int sellPrice = (int)((double)item.getPrice() * 0.8);// sell price is always 80% of buy price
		int numCoins = 0;
		if (ItemDao.itemHasAttribute(item.getItemId(), ItemAttributes.STACKABLE)) {
			// if the inventory is full and we don't have coins, but we're selling the whole stack
			// then we can replace the stack with the coins.
			InventoryItemDto invItem = PlayerStorageDao.getInventoryItemFromPlayerIdAndSlot(player.getId(), invItemIds.indexOf(item.getItemId()));
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
			PlayerStorageDao.setCountOnInventorySlot(player.getId(), invItemIds.indexOf(item.getItemId()), invItem.getCount() - sellCount);
			
					
		} else {
			int sellCount = Math.min(Collections.frequency(invItemIds, item.getItemId()), request.getAmount());
			numCoins = sellCount * sellPrice;
			
			for (int i = 0; i < invItemIds.size() && sellCount > 0; ++i) {
				if (invItemIds.get(i) == item.getItemId()) {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), i, 0, 1);
					--sellCount;
				}
			}
		}
		
		// items were sold, now give the player the coins
		if (invItemIds.contains(Items.COINS.getValue()))
			PlayerStorageDao.addCountToInventoryItemSlot(player.getId(), invItemIds.indexOf(Items.COINS.getValue()), numCoins);
		else
			PlayerStorageDao.addItemByPlayerIdItemId(player.getId(), Items.COINS.getValue(), numCoins);
		
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
	}
	
}
