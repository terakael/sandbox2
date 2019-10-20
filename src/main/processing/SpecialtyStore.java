package main.processing;

import main.database.ShopDto;
import main.database.ShopItemDto;

public class SpecialtyStore extends Store {

	public SpecialtyStore(ShopDto dto) {
		super(dto);
	}

	@Override
	public int getShopSellPriceAt(ShopItemDto item, int currentStock) {
		double price = (double)item.getPrice();
		for (int i = 0; i <= currentStock; ++i)
			price *= 0.92;
		
		return (int)price;
	}
	
	@Override
	public int getShopBuyPriceAt(ShopItemDto item, int currentStock) {
		double price = (double)item.getPrice();
		for (int i = 0; i <= currentStock; ++i)
			price *= 0.88;
		
		return (int)price;
	}

}
