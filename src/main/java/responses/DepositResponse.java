package responses;

import database.dao.SceneryDao;
import processing.PathFinder;
import processing.attackable.Player;
import requests.DepositRequest;
import requests.Request;

public class DepositResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		DepositRequest request = (DepositRequest)req;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId()))
			return; // how did the player get away from the deposit thing?
		
		switch (SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId())) {
		case 53: // bank chest
			new BankDepositResponse().process(req, player, responseMaps);
			return;
			
		case 141: // small storage chest
		case 147: // large storage chest
			new StorageChestDepositResponse().process(req, player, responseMaps);
			return;
		}
	}

}
