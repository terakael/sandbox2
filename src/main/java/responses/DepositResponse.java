package responses;

import database.dao.SceneryDao;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Ship;
import processing.managers.ShipManager;
import requests.DepositRequest;
import requests.Request;

public class DepositResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		DepositRequest request = (DepositRequest)req;
		
		// check ship storage first
		Ship ship = ShipManager.getShipByCaptainId(player.getId());
		if (ship != null && ship.getTileId() == request.getTileId() && (PathFinder.isAdjacent(player.getTileId(), request.getTileId()) || ShipManager.getShipWithPlayer(player) == ship)) {
			new ShipDepositResponse().process(req, player, responseMaps);
			return;
		}
		
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
