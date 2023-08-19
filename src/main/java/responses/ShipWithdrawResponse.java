package responses;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import processing.attackable.Ship;
import processing.managers.ConstructableManager;
import processing.managers.ShipManager;
import processing.scenery.constructable.Constructable;
import processing.scenery.constructable.StorageChest;
import requests.Request;
import requests.WithdrawRequest;
import types.ItemAttributes;
import types.StorageTypes;

@SuppressWarnings("unused")
public class ShipWithdrawResponse extends StorageWithdrawResponse {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		WithdrawRequest request = (WithdrawRequest)req;
		
		final Ship ship = ShipManager.getShipByCaptainId(player.getId());
		if (ship == null) {
			// player doesn't have a ship
			return;
		}
		withdraw(player, ship.getStorage(), request, responseMaps);
	}

}
