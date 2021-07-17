package main.processing.stores;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import main.database.dao.ItemDao;
import main.database.dto.ItemDto;
import main.database.dto.ShopDto;
import main.database.dto.ShopItemDto;
import main.responses.ResponseMaps;

public abstract class Store {
	@Getter protected ShopDto dto;
	@Getter protected int shopId;
	@Getter protected int ownerId;
	protected ArrayList<ShopItemDto> baseItems = new ArrayList<>();
	@Getter @Setter protected boolean dirty = false;
	protected final int maxStock = 16;
	protected int tickCounter = 0;
	
	public Store(ShopDto dto) {
		this.dto = dto;
		this.shopId = dto.getId();
		this.ownerId = dto.getOwnerId();
		this.baseItems = dto.getItems();
	}
	
	public void process(ResponseMaps responseMaps) {
		if (++tickCounter == Integer.MAX_VALUE)
			tickCounter = 0;
		
		for (ShopItemDto item : baseItems) {
			if (tickCounter % item.getRespawnTicks() == 0) {
				if (item.getCurrentStock() < item.getMaxStock())
					addItem(item.getItemId(), 1);
				else if (item.getCurrentStock() > item.getMaxStock())
					decreaseItemStock(item.getItemId(), 1);
			}
		}
	}
	
	public HashMap<Integer, ShopItemDto> getStock() {
		HashMap<Integer, ShopItemDto> stock = new HashMap<>();
		int slot = 0;
		for (int i = 0; i < baseItems.size(); ++i)
			stock.put(slot++, baseItems.get(i));
		
		return stock;
	}
	
	public void addItem(int itemId, int count) {
		ItemDto item = ItemDao.getItem(itemId);
		if (item == null)
			return;
		
		dirty = true;
		
		// check base items first
		for (ShopItemDto stock : baseItems) {
			if (stock.getItemId() == itemId) {
				stock.setCurrentStock(stock.getCurrentStock() + count);
				return;
			}
		}
	}
	
	public void decreaseItemStock(int itemId, int amount) {
		ShopItemDto item = getStockByItemId(itemId);
		if (item != null) {
			item.setCurrentStock(item.getCurrentStock() - amount);
			dirty = true;
		}
	}
	
	public ShopItemDto getStockByItemId(int itemId) {
		for (ShopItemDto stock : baseItems) {
			if (stock.getItemId() == itemId)
				return stock;
		}	
		return null;
	}
	
	public boolean isFull() {
		return baseItems.size() >= maxStock;
	}
	
	public boolean hasStock(int itemId) {
		return getStockByItemId(itemId) != null;
	}
	
	public boolean buysItem(int itemId) {
		return hasStock(itemId);
	}
	
	public int getShopSellPriceAt(ShopItemDto item, int currentStock) {
		double price = (double)item.getPrice();
		for (int i = 0; i <= currentStock; ++i)
			price *= 0.95;
		
		return Math.max((int)((double)item.getPrice() * 0.15) + 1, (int)price);
	}
	
	public int getShopSellPrice(ShopItemDto item) {
		return getShopSellPriceAt(item, item.getCurrentStock());	
	}
	
	public int getShopBuyPriceAt(ShopItemDto item, int currentStock) {
		double price = (double)item.getPrice();
		for (int i = 0; i <= currentStock; ++i)
			price *= 0.85;
		
		return Math.max((int)((double)item.getPrice() * 0.12) + 1, (int)price);
	}
	
	public int getShopBuyPrice(ShopItemDto item) {
		return getShopBuyPriceAt(item, item.getCurrentStock());
	}
}
