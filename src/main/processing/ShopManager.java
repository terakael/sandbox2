package main.processing;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;
import main.database.ShopDao;
import main.responses.ResponseMaps;

public class ShopManager {
	private static int UPDATE_TIMER = 100;
	private static int updateTime = UPDATE_TIMER;
	@Getter private static ArrayList<Store> shops = new ArrayList<>();
	
	public static void setupShops() {
		for (HashMap.Entry<Integer, Integer> entry : ShopDao.getShopIdsByOwnerId().entrySet()) {
			if (entry.getValue() == 1) // storeid 1 is general store, the rest are specialty. todo pull store type from stores table
				shops.add(new GeneralStore(entry.getValue(), entry.getKey(), ShopDao.getShopStockById(entry.getValue())));
			else
				shops.add(new SpecialtyStore(entry.getValue(), entry.getKey(), ShopDao.getShopStockById(entry.getValue())));
		}
	}
	
	public static void process(ResponseMaps responseMaps) {
		if (--updateTime > 0)
			return;
		
		updateTime = UPDATE_TIMER;
		
		for (Store shop : shops) {
			shop.process(responseMaps);
		}
	}
	
	public static void addItem(int shopId, int itemId, int count) {
		for (Store shop : shops) {
			if (shop.getShopId() == shopId) {
				shop.addItem(itemId, count);
			}
		}
	}
	
	public static Store getShopByOwnerId(int ownerId) {
		for (Store shop : shops) {
			if (shop.getOwnerId() == ownerId)
				return shop;
		}
		return null;
	}
	
	public static Store getShopByShopId(int shopId) {
		for (Store shop : shops) {
			if (shop.getShopId() == shopId)
				return shop;
		}
		return null;
	}
}
