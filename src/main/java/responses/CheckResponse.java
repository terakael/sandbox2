package responses;

import java.util.Map;
import java.util.stream.Collectors;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import requests.CheckRequest;
import requests.Request;
import types.StorageTypes;

public class CheckResponse extends Response {
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true; // checking the sack during combat is fine
	}
	
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
							.map(e -> String.format("%dx %s", e.getCount(), ItemDao.getNameFromId(e.getItemId(), e.getCount() != 1)))
							.collect(Collectors.joining(", ")), 
						"white"));
				
				
			}
			break;
		}
	}
	
}
