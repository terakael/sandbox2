package main.processing;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import main.database.ItemDao;
import main.database.ItemDto;
import main.database.ShopDto;
import main.responses.ResponseMaps;

public abstract class Store {
	@Getter protected int shopId;
	@Getter protected int ownerId;
	protected ArrayList<ShopDto> baseItems = new ArrayList<>();
	@Getter @Setter protected boolean dirty = false;
	protected final int maxStock = 16;
	
	public Store(int shopId, int ownerId, ArrayList<ShopDto> baseItems) {
		this.shopId = shopId;
		this.ownerId = ownerId;
		this.baseItems = baseItems;
	}
	
	public void process(ResponseMaps responseMaps) {
		for (ShopDto item : baseItems) {
			if (item.getCurrentStock() < item.getMaxStock())
				addItem(item.getItemId(), 1);
			else if (item.getCurrentStock() > item.getMaxStock())
				decreaseItemStock(item.getItemId(), 1);
		}
	}
	
	public HashMap<Integer, ShopDto> getStock() {
		HashMap<Integer, ShopDto> stock = new HashMap<>();
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
		for (ShopDto stock : baseItems) {
			if (stock.getItemId() == itemId) {
				stock.setCurrentStock(stock.getCurrentStock() + count);
				return;
			}
		}
	}
	
	public void decreaseItemStock(int itemId, int amount) {
		ShopDto item = getStockByItemId(itemId);
		if (item != null) {
			item.setCurrentStock(item.getCurrentStock() - amount);
			dirty = true;
		}
	}
	
	public ShopDto getStockByItemId(int itemId) {
		for (ShopDto stock : baseItems) {
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
}
