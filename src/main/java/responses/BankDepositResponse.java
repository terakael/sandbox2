package responses;

import database.dao.SceneryDao;
import processing.PathFinder;
import processing.attackable.Player;
import processing.managers.BankManager;
import requests.DepositRequest;
import requests.Request;

public class BankDepositResponse extends StorageDepositResponse {

	@Override
	public void process(Request request, Player player, ResponseMaps responseMaps) {
		final int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (sceneryId != 53)// storage chest scenery id
			return;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId()))
			return;
		
		deposit(player, BankManager.getStorage(player.getId()), (DepositRequest)request, responseMaps);
	}
}