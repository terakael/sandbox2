package responses;

import java.util.Collections;
import java.util.List;

import database.dao.ArtisanEnhanceableItemsDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dto.ArtisanEnhanceableItemsDto;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import requests.EnhanceItemRequest;
import requests.Request;
import types.ArtisanShopTabs;
import types.StorageTypes;

public class EnhanceItemResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof EnhanceItemRequest))
			return;
		
		EnhanceItemRequest request = (EnhanceItemRequest)req;
		final int amount = request.getAmount() == -1 ? Integer.MAX_VALUE : request.getAmount();
		
		if (!ArtisanManager.playerIsNearMaster(player))
			return;
		
		ArtisanEnhanceableItemsDto enhancedItemDto = ArtisanEnhanceableItemsDao.getEnhanceableItems().get(request.getItemId());
		if (enhancedItemDto == null)
			return;
		
		final int totalPoints = ArtisanManager.getTotalPointsByPlayerId(player.getId());
		final int numEnhancementsPlayerCanAfford = totalPoints / enhancedItemDto.getNumPoints();
		if (numEnhancementsPlayerCanAfford == 0) {
			setRecoAndResponseText(0, "you don't have enough points.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		int numItemsToEnhance = Math.min(amount, Collections.frequency(invItemIds, request.getItemId()));		
		numItemsToEnhance = Math.min(numItemsToEnhance, numEnhancementsPlayerCanAfford);
		
		for (int i = 0; i < numItemsToEnhance; ++i) {
			int nonEnhancedItemIndex = invItemIds.indexOf(request.getItemId());
			if (nonEnhancedItemIndex >= 0) {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, nonEnhancedItemIndex, enhancedItemDto.getEnhancedItemId(), 1, ItemDao.getMaxCharges(enhancedItemDto.getEnhancedItemId()));
				invItemIds.set(nonEnhancedItemIndex, 0);
			}
		}
		
		final int numPointsToSpend = numItemsToEnhance * enhancedItemDto.getNumPoints();
		if (totalPoints == numPointsToSpend) {
			setRecoAndResponseText(0, "you have run out of points.");
			responseMaps.addClientOnlyResponse(player, this);
		}
		
		ArtisanManager.spendPoints(player.getId(), numPointsToSpend);
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		// "update" the stock by resending the show request
		new ShowArtisanShopResponse(ArtisanShopTabs.enhance).process(null, player, responseMaps);
	}

}
