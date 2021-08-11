package responses;

import java.util.List;

import database.dao.ArtisanShopStockDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import requests.BuyArtisanStockRequest;
import requests.Request;
import types.ArtisanShopTabs;
import types.StorageTypes;

public class BuyArtisanStockResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof BuyArtisanStockRequest))
			return;
		
		BuyArtisanStockRequest request = (BuyArtisanStockRequest)req;
		if (!ArtisanManager.playerIsNearMaster(player))
			return;
		
		Integer stockPrice = ArtisanShopStockDao.getShopStock().get(request.getItemId());
		if (stockPrice == null)
			return; // wasn't in stock map, therefore invalid item
		
		final int totalPoints = ArtisanManager.getTotalPointsByPlayerId(player.getId());
		if (totalPoints < stockPrice) {
			setRecoAndResponseText(0, "you don't have enough points to buy that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!invItemIds.contains(0)) {
			setRecoAndResponseText(0, "you don't have enough inventory space.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		ArtisanManager.spendPoints(player.getId(), stockPrice);
		PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, request.getItemId(), 1, ItemDao.getMaxCharges(request.getItemId()));
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		// "update" the stock by resending the show request
		new ShowArtisanShopResponse(ArtisanShopTabs.shop).process(null, player, responseMaps);
	}

}
