package main.responses;

import main.database.ItemDao;
import main.database.ItemDto;
import main.database.ShopItemDto;
import main.processing.Player;
import main.processing.ShopManager;
import main.processing.Store;
import main.requests.Request;
import main.requests.ShopValueRequest;
import main.types.ItemAttributes;
import main.types.Items;

public class ShopValueResponse extends Response {
	public ShopValueResponse() {
		setAction("value");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ShopValueRequest))
			return;
		
		ShopValueRequest request = (ShopValueRequest)req;
		
//		ShopDto item = null;
//		for (ShopDto dto : ShopDao.getShopStockById(player.getShopId())) {
//			if (dto.getItemId() == request.getObjectId()) {
//				item = dto;
//				break;
//			}
//		}
		
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
			shopItem = new ShopItemDto(item.getId(), 0, 0, item.getPrice());// default; a general store with no stock
		
		int value = shop.getShopSellPrice(shopItem);
		if (request.getValueTypeId() == 1)
			value = shop.getShopBuyPrice(shopItem);
		
		setRecoAndResponseText(1, String.format("%s is %s for %d coin%s.", 
				ItemDao.getNameFromId(item.getId()), 
				request.getValueTypeId() == 0 ? "sold" : "bought",
				value, 
				value == 1 ? "" : "s"));
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
}
