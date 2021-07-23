package processing.stores;

import database.dto.ShopDto;
import database.dto.ShopItemDto;

public class SpecialtyStore extends Store {

	public SpecialtyStore(ShopDto dto) {
		super(dto);
	}

	@Override
	public int getShopSellPriceAt(ShopItemDto item, int currentStock) {
		double price = (double)item.getPrice();
		for (int i = 0; i <= currentStock; ++i)
			price *= 0.92;
		
		return Math.max((int)((double)item.getPrice() * 0.13) + 1, (int)price);
	}
	
	@Override
	public int getShopBuyPriceAt(ShopItemDto item, int currentStock) {
		double price = (double)item.getPrice();
		for (int i = 0; i <= currentStock; ++i)
			price *= 0.88;
		
		return Math.max((int)((double)item.getPrice() * 0.1) + 1, (int)price);
	}

}
