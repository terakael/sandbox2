package responses;

import database.dao.ItemDao;
import database.dto.ItemDto;
import database.dto.ShopItemDto;
import processing.attackable.Player;
import processing.managers.ShopManager;
import processing.stores.Store;
import requests.Request;
import requests.ShopValueRequest;
import types.ItemAttributes;
import types.Items;

public class ShopValueResponse extends Response {
	public ShopValueResponse() {
		setAction("value");
		setCombatInterrupt(false);
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ShopValueRequest))
			return;
		
		ShopValueRequest request = (ShopValueRequest)req;
		
		Store shop = ShopManager.getShopByShopId(player.getShopId());
		if (shop == null)
			return;
		
		ItemDto item = ItemDao.getItem(request.getObjectId());
		
		if (item == null 
				|| item.getId() == Items.COINS.getValue()
				|| !ItemDao.itemHasAttribute(item.getId(), ItemAttributes.TRADEABLE) 
				|| ItemDao.itemHasAttribute(item.getId(), ItemAttributes.UNIQUE)
				|| !shop.buysItem(item.getId())) {
			setRecoAndResponseText(1, "you can't sell that here.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		ShopItemDto shopItem = shop.getStockByItemId(item.getId());
		if (shopItem == null)
			shopItem = new ShopItemDto(item.getId(), 0, 0, item.getPrice(), 100);// default; a general store with no stock
		
		int value = shop.getShopSellPrice(shopItem);
		if (request.getValueTypeId() == 1)
			value = shop.getShopBuyPrice(shopItem);
		
		setRecoAndResponseText(1, String.format("%s can be %s for %d coin%s.", 
				ItemDao.getNameFromId(item.getId(), true), 
				request.getValueTypeId() == 0 ? "bought" : "sold",
				value, 
				value == 1 ? "" : "s"));
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
}
