package main.responses;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dto.InventoryItemDto;
import main.processing.ClientResourceManager;
import main.processing.Player;
import main.requests.EmptyRequest;
import main.requests.Request;
import main.types.Items;
import main.types.StorageTypes;

public class EmptyResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof EmptyRequest))
			return;
		
		List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		if (!invItemIds.contains(Items.FLOWER_SACK.getValue()))
			return;
		
		Map<Integer, InventoryItemDto> flowerSackContents = PlayerStorageDao.getStorageDtoMapByPlayerIdExcludingEmpty(player.getId(), StorageTypes.FLOWER_SACK);
		int totalFlowers = flowerSackContents.values().stream().reduce(0, (cumulative, item) -> cumulative + item.getCount(), Integer::sum);
		if (totalFlowers == 0) {
			setRecoAndResponseText(0, "the sack is already empty.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (!invItemIds.contains(0)) {
			setRecoAndResponseText(0, "you don't have any inventory space.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		Set<Integer> withdrawnFlowerIds = new HashSet<>(); // for sending the client the flower sprites if they haven't loaded them yet
		// whatever is smaller: remaining inventory space, or remaining flowers
		int withdrawAmount = Math.min(totalFlowers, Collections.frequency(invItemIds, 0));
		for (Map.Entry<Integer, InventoryItemDto> entry : flowerSackContents.entrySet()) {
			withdrawnFlowerIds.add(entry.getValue().getItemId());
			int flowersToAdd = Math.min(entry.getValue().getCount(), withdrawAmount);
			
			for (int i = 0; i < flowersToAdd; ++i)
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, entry.getValue().getItemId(), 1, ItemDao.getMaxCharges(entry.getValue().getItemId()));
			PlayerStorageDao.setCountOnSlot(player.getId(), StorageTypes.FLOWER_SACK, entry.getKey(), entry.getValue().getCount() - flowersToAdd);
			
			withdrawAmount -= flowersToAdd;
			if (withdrawAmount == 0)
				break;
		};
		
		if (!withdrawnFlowerIds.isEmpty())
			ClientResourceManager.addItems(player, withdrawnFlowerIds);
		
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
	}

}
