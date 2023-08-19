package responses;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import processing.managers.ConstructableManager;
import processing.scenery.constructable.Constructable;
import processing.scenery.constructable.StorageChest;
import requests.Request;
import requests.WithdrawRequest;
import types.ItemAttributes;
import types.StorageTypes;

@SuppressWarnings("unused")
public class StorageChestWithdrawResponse extends StorageWithdrawResponse {
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		WithdrawRequest request = (WithdrawRequest)req;
		
		final Constructable constructable = ConstructableManager.getConstructableInstanceByTileId(player.getFloor(), request.getTileId());
		if (constructable == null)
			return;
		
		if (!(constructable instanceof StorageChest))
			return;
		
		StorageChest chest = (StorageChest)constructable;
		withdraw(player, chest.getPlayerStorage(player.getId()), request, responseMaps);
	}

}
