package main.responses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.mysql.cj.x.protobuf.MysqlxCrud.Collection;

import main.database.InventoryItemDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.database.ShopDao;
import main.database.ShopItemDto;
import main.processing.GeneralStore;
import main.processing.Player;
import main.processing.ShopManager;
import main.processing.Store;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.ShopBuyRequest;
import main.types.ItemAttributes;
import main.types.Items;
import main.types.StorageTypes;

public class ShopBuyResponse extends Response {
	public ShopBuyResponse() {
		setAction("buy");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ShopBuyRequest))
			return;
		
		ShopBuyRequest request = (ShopBuyRequest)req;
		int requestAmount = Math.min(request.getAmount(), 10);// caps at 10, so they cant inject any other values from the client
		
		Store shop = ShopManager.getShopByShopId(player.getShopId());
		if (shop == null) {
			return;
		}
		
		ShopItemDto item = shop.getStockByItemId(request.getObjectId());
		
//		ShopDto item = null;
//		for (ShopDto dto : ShopDao.getShopStockById(player.getShopId())) {
//			if (dto.getItemId() == request.getObjectId()) {
//				item = dto;
//				break;
//			}
//		}
		
		if (item == null) {
			setRecoAndResponseText(0, "you can't buy that here.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		ArrayList<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
		if (!invItemIds.contains(Items.COINS.getValue())) { // coins
			setRecoAndResponseText(0, "you need coins to buy items.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		int price = shop.getShopSellPrice(item);
		
		InventoryItemDto coins = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), invItemIds.indexOf(Items.COINS.getValue()));
		if (coins.getCount() < price) {// if you can't buy a single item, return this message
			setRecoAndResponseText(0, "you don't have enough to buy that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
				
//		int amountPlayerCanAfford = Math.min(request.getAmount(), item.getPrice() == 0 ? request.getAmount() : coins.getCount() / item.getPrice());
		if (ItemDao.itemHasAttribute(item.getItemId(), ItemAttributes.STACKABLE)) {
			int[] out = {0, 0};
			calculateWhatPlayerCanAfford(requestAmount, coins.getCount(), shop, item, out);
			int amountPlayerCanAfford = out[1];
			int coinsToSpend = out[0];
			
//			int actualAmount = Math.min(amountPlayerCanAfford, item.getCurrentStock());
			PlayerStorageDao.setCountOnInventorySlot(player.getId(), coins.getSlot(), coins.getCount() - coinsToSpend);
			if (invItemIds.contains(item.getItemId())) {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY.getValue(), invItemIds.indexOf(item.getItemId()), amountPlayerCanAfford);
			} else {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), item.getItemId(), amountPlayerCanAfford);
//				PlayerStorageDao.addItemByPlayerIdItemId(player.getId(), item.getItemId(), actualAmount);
			}
			shop.decreaseItemStock(item.getItemId(), amountPlayerCanAfford);
		} else {
			int count = Math.min(Collections.frequency(invItemIds, 0), requestAmount);
			count = Math.min(count, item.getCurrentStock());
			
			int[] out = {0, 0};
			calculateWhatPlayerCanAfford(count, coins.getCount(), shop, item, out);
			int amountPlayerCanAfford = out[1];
			int coinsToSpend = out[0];
			
			
			
			PlayerStorageDao.setCountOnInventorySlot(player.getId(), coins.getSlot(), coins.getCount() - coinsToSpend);
			shop.decreaseItemStock(item.getItemId(), amountPlayerCanAfford);
			
			for (int i = 0; i < invItemIds.size() && amountPlayerCanAfford > 0; ++i) {
				if (invItemIds.get(i) == 0) {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), i, item.getItemId(), ItemDao.getMaxCharges(item.getItemId()));
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
