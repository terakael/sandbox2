package main.responses;

import main.database.ItemDao;
import main.database.ItemDto;
import main.processing.Player;
import main.processing.ShopManager;
import main.processing.Store;
import main.requests.Request;
import main.requests.ShopValueRequest;
import main.types.ItemAttributes;

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
				|| !ItemDao.itemHasAttribute(item.getId(), ItemAttributes.TRADEABLE) 
				|| ItemDao.itemHasAttribute(item.getId(), ItemAttributes.UNIQUE)
				|| !shop.buysItem(item.getId())) {
			setRecoAndResponseText(1, "you can't sell that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		int value = item.getPrice();
		if (request.getValueTypeId() == 1)
			value = (int)((double)value * 0.8);
		
		setRecoAndResponseText(1, String.format("%s is %s for %d coin%s.", 
				ItemDao.getNameFromId(item.getId()), 
				request.getValueTypeId() == 0 ? "sold" : "bought",
				value, 
				value == 1 ? "" : "s"));
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
}
