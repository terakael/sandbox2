package main.responses;

import main.database.dao.SceneryDao;
import main.processing.PathFinder;
import main.processing.Player;
import main.requests.DepositRequest;
import main.requests.Request;

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
			new StorageChestDepositResponse().process(req, player, responseMaps);
			return;
		}
	}

}
