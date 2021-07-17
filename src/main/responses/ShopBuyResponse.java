package main.responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dto.InventoryItemDto;
import main.database.dto.ShopItemDto;
import main.processing.attackable.Player;
import main.processing.managers.ShopManager;
import main.processing.stores.Store;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.ShopBuyRequest;
import main.types.ItemAttributes;
import main.types.Items;
import main.types.StorageTypes;

public class ShopBuyResponse extends Response {
	// if the client tries to send a different amount, then fail.  These are the only allowed amounts.
	private static Set<Integer> allowedRequestAmounts = new HashSet<>(Arrays.asList(1, 5, 10, 50));
		
	public ShopBuyResponse() {
		setAction("buy");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ShopBuyRequest))
			return;
		
		final ShopBuyRequest request = (ShopBuyRequest)req;
		int requestAmount = request.getAmount();
		if (!allowedRequestAmounts.contains(requestAmount))
			return;
		
		Store shop = ShopManager.getShopByShopId(player.getShopId());
		if (shop == null) {
			// due to client fuckery, don't send them a response.
			return;
		}
		
		ShopItemDto item = shop.getStockByItemId(request.getObjectId());
		if (item == null) {
			// due to client fuckery, don't send them a response.
			return;
		}
		
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!invItemIds.contains(Items.COINS.getValue())) {
			setRecoAndResponseText(0, "you need coins to buy items.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		final int price = shop.getShopSellPrice(item);
		
		InventoryItemDto coins = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, invItemIds.indexOf(Items.COINS.getValue()));		
		if (coins.getCount() < price) {// if you can't buy a single item, return this message
			setRecoAndResponseText(0, "you don't have enough to buy that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (ItemDao.itemHasAttribute(item.getItemId(), ItemAttributes.STACKABLE)) {
			int[] out = {0, 0};
			calculateWhatPlayerCanAfford(requestAmount, coins.getCount(), shop, item, out);
			int amountPlayerCanAfford = out[1];
			int coinsToSpend = out[0];
			
			PlayerStorageDao.setCountOnSlot(player.getId(), StorageTypes.INVENTORY, coins.getSlot(), coins.getCount() - coinsToSpend);
			if (invItemIds.contains(item.getItemId())) {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY, invItemIds.indexOf(item.getItemId()), amountPlayerCanAfford);
			} else {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, item.getItemId(), amountPlayerCanAfford, 0);
			}
			shop.decreaseItemStock(item.getItemId(), amountPlayerCanAfford);
		} else {
			// cap the amount at the number of total free inventory spaces
			int count = Math.min(Collections.frequency(invItemIds, 0), requestAmount);
			
			// then further cap the amount at however many items are in stock
			count = Math.min(count, item.getCurrentStock());
			
			int[] out = {0, 0};
			calculateWhatPlayerCanAfford(count, coins.getCount(), shop, item, out);
			int amountPlayerCanAfford = out[1];
			int coinsToSpend = out[0];
			
			PlayerStorageDao.setCountOnSlot(player.getId(), StorageTypes.INVENTORY, coins.getSlot(), coins.getCount() - coinsToSpend);
			shop.decreaseItemStock(item.getItemId(), amountPlayerCanAfford);
			
			for (int i = 0; i < invItemIds.size() && amountPlayerCanAfford > 0; ++i) {
				if (invItemIds.get(i) == 0) {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, i, item.getItemId(), 1, ItemDao.getMaxCharges(item.getItemId()));
					--amountPlayerCanAfford;
				}
			}
		}
		
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
	}
	
	private void calculateWhatPlayerCanAfford(int requestAmount, int coinCount, Store shop, ShopItemDto item, int[] output) {
		// pretty dirty hack for the output but basically i need two things returned:
		// [0] how many coins i'll spend
		// [1] how many items i'll purchase
		requestAmount = Math.min(requestAmount, item.getCurrentStock());// can't buy more than the max stock
		
		for (int i = 0; i < requestAmount; ++i, ++output[1]) {
			int currentPrice = shop.getShopSellPriceAt(item, item.getCurrentStock() - i); 
			if (coinCount - currentPrice < 0)
				break;
			
			coinCount -= currentPrice;
			output[0] += currentPrice;
		}
	}

}
