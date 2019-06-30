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
		
		InventoryItemDto coins = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), invItemIds.indexOf(Items.COINS.getValue()));
		if (coins.getCount() < item.getPrice()) {// if you can't buy a single item, return this message
			setRecoAndResponseText(0, "you don't have enough to buy that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// if you can buy at least one, but not necessarily the amount requested
		int amountPlayerCanAfford = Math.min(request.getAmount(), item.getPrice() == 0 ? request.getAmount() : coins.getCount() / item.getPrice());
		if (ItemDao.itemHasAttribute(item.getItemId(), ItemAttributes.STACKABLE)) {
			int actualAmount = Math.min(amountPlayerCanAfford, item.getCurrentStock());
			PlayerStorageDao.setCountOnInventorySlot(player.getId(), coins.getSlot(), coins.getCount() - (actualAmount * item.getPrice()));
			if (invItemIds.contains(item.getItemId())) {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY.getValue(), invItemIds.indexOf(item.getItemId()), actualAmount);
			} else {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), item.getItemId(), actualAmount);
//				PlayerStorageDao.addItemByPlayerIdItemId(player.getId(), item.getItemId(), actualAmount);
			}
			shop.decreaseItemStock(item.getItemId(), actualAmount);
		} else {
			int count = Math.min(Collections.frequency(invItemIds, 0), amountPlayerCanAfford);
			count = Math.min(count, item.getCurrentStock());
			
			PlayerStorageDao.setCountOnInventorySlot(player.getId(), coins.getSlot(), coins.getCount() - (count * item.getPrice()));
			shop.decreaseItemStock(item.getItemId(), count);
			
			for (int i = 0; i < invItemIds.size() && count > 0; ++i) {
				if (invItemIds.get(i) == 0) {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), i, item.getItemId(), 1);
					--count;
				}
			}
		}
		
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
	}

}
