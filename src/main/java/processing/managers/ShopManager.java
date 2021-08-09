package processing.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.dao.ShopDao;
import database.dto.ShopDto;
import lombok.Getter;
import processing.stores.GeneralStore;
import processing.stores.SpecialtyStore;
import processing.stores.Store;
import responses.ResponseMaps;
import types.ShopTypes;

public class ShopManager {
	@Getter private static Map<Integer, Store> shopsByShopId = new HashMap<>();
	
	public static void setupShops() {
		ArrayList<ShopDto> shopDtos = ShopDao.getShopsAndItems();
		
		for (ShopDto dto : shopDtos) {
			ShopTypes type = ShopTypes.withValue(dto.getShopType());
			if (type == null)
				continue;
			
			switch (type) {
			case GENERAL: {//general
				shopsByShopId.put(dto.getId(), new GeneralStore(dto));
				break;
			}
			
			case SPECIALTY: {//specialty
				shopsByShopId.put(dto.getId(), new SpecialtyStore(dto));
				break;
			}
			
			default:
				break;
			}
		}
	}
	
	public static void process(ResponseMaps responseMaps) {
		shopsByShopId.forEach((id, shop) -> shop.process(responseMaps));
	}
	
	public static void addItem(int shopId, int itemId, int count) {
		if (shopsByShopId.containsKey(shopId))
			shopsByShopId.get(shopId).addItem(itemId, count);
	}
	
	public static Store getShopByOwnerId(int ownerId) {
		return shopsByShopId.values().stream()
				.filter(e -> e.getOwnerId() == ownerId)
				.findFirst()
				.orElse(null);
	}
	
	public static Store getShopByShopId(int shopId) {
		return shopsByShopId.get(shopId);
	}
	
	public static List<Store> getAllShops() {
		return new ArrayList<>(shopsByShopId.values());
	}
}
