package main.processing;

import java.util.ArrayList;

import main.database.ShopDto;

public class SpecialtyStore extends Store {

	public SpecialtyStore(int shopId, int ownerId, ArrayList<ShopDto> baseItems) {
		super(shopId, ownerId, baseItems);
	}

}
