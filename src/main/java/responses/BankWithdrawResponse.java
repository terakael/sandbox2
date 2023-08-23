package responses;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.InventoryItemDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.managers.BankManager;
import requests.Request;
import requests.RequestFactory;
import requests.WithdrawRequest;
import types.ItemAttributes;
import types.StorageTypes;

@SuppressWarnings("unused")
public class BankWithdrawResponse extends StorageWithdrawResponse {

	@Override
	public void process(Request request, Player player, ResponseMaps responseMaps) {
		final int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (sceneryId != 53)// storage chest scenery id
			return;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId()))
			return;
		
		withdraw(player, BankManager.getStorage(player.getId()), (WithdrawRequest)request, responseMaps);
	}
}
