package main.responses;

import java.util.Map;
import java.util.stream.Collectors;

import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dto.InventoryItemDto;
import main.processing.Player;
import main.requests.CheckRequest;
import main.requests.Request;
import main.types.StorageTypes;

public class CheckResponse extends Response {
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof CheckRequest))
			return;
		
		switch (PlayerStorageDao.getItemIdInSlot(player.getId(), StorageTypes.INVENTORY, ((CheckRequest)req).getSlot())) {
		case 374: // flower sack
			Map<Integer, InventoryItemDto> sackContents = PlayerStorageDao.getStorageDtoMapByPlayerIdExcludingEmpty(player.getId(), StorageTypes.FLOWER_SACK);
			if (sackContents.isEmpty()) {
				responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("the sack is empty.", "white"));
			} else {
				responseMaps.addClientOnlyResponse(player, 
						MessageResponse.newMessageResponse("the sack contains " + sackContents.values().stream()
							.map(e -> String.format("%dx %s", e.getCount(), ItemDao.getNameFromId(e.getItemId())))
							.collect(Collectors.joining(", ")), 
						"white"));
				
				
			}
			break;
		}
	}
	
}
