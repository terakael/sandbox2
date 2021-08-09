package responses;

import java.util.HashMap;
import java.util.stream.Collectors;

import database.dao.ShopDao;
import database.dto.ShopItemDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import processing.stores.Store;
import requests.Request;

public class ShowShopResponse extends Response {
	private transient Store shop = null;
	private HashMap<Integer, ShopItemDto> shopStock;
	private String shopName;
	
	public ShowShopResponse(Store shop) {
		setAction("show_shop");
		this.shop = shop;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (shop == null)
			return;
		
		player.setShopId(shop.getShopId());
		shopStock = shop.getStock();
		shopName = ShopDao.getShopNameById(shop.getShopId());
		
		ClientResourceManager.addItems(player, shopStock.values().stream().map(ShopItemDto::getItemId).collect(Collectors.toSet()));
		responseMaps.addClientOnlyResponse(player, this);
	}

}
