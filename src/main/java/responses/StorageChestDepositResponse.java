package responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import database.dao.EquipmentDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import processing.managers.ConstructableManager;
import processing.scenery.constructable.Constructable;
import processing.scenery.constructable.StorageChest;
import requests.DepositRequest;
import requests.Request;
import types.ItemAttributes;
import types.Storage;
import types.StorageTypes;

@SuppressWarnings("unused")
public class StorageChestDepositResponse extends StorageDepositResponse {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		DepositRequest request = (DepositRequest)req;
		Constructable constructable = ConstructableManager.getConstructableInstanceByTileId(player.getFloor(), request.getTileId());
		if (constructable == null)
			return;
		
		if (!(constructable instanceof StorageChest))
			return;
		
		deposit(player, ((StorageChest)constructable).getPlayerStorage(player.getId()), request, responseMaps);
	}

}
