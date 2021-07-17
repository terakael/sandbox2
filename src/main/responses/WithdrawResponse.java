package main.responses;

import main.database.dao.SceneryDao;
import main.processing.PathFinder;
import main.processing.attackable.Player;
import main.requests.Request;
import main.requests.WithdrawRequest;

public class WithdrawResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		WithdrawRequest request = (WithdrawRequest)req;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId()))
			return; // how did the player get away from the deposit place?
		
		switch (SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId())) {
		case 53: // bank chest
			new BankWithdrawResponse().process(req, player, responseMaps);
			return;
			
		case 141: // small storage chest
		case 147: // large storage chest
			new StorageChestWithdrawResponse().process(req, player, responseMaps);
			return;
		}
	}

}
