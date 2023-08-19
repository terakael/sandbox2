package responses;

import database.dao.SceneryDao;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Ship;
import processing.managers.ShipManager;
import requests.Request;
import requests.WithdrawRequest;

public class WithdrawResponse extends Response {
	public WithdrawResponse() {
		setCombatInterrupt(false);
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		WithdrawRequest request = (WithdrawRequest)req;
		
		// check ship storage first
		Ship ship = ShipManager.getShipByCaptainId(player.getId());
		if (ship != null && ship.getTileId() == request.getTileId() && (PathFinder.isAdjacent(player.getTileId(), request.getTileId()) || ShipManager.getShipWithPlayer(player) == ship)) {
			new ShipWithdrawResponse().process(req, player, responseMaps);
			return;
		}
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId()))
			return; // how did the player get away from the deposit place?
		
		switch (SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId())) {
		case 53: // bank chest
			new BankWithdrawResponse().processSuper(req, player, responseMaps);
			return;
			
		case 141: // small storage chest
		case 147: // large storage chest
			new StorageChestWithdrawResponse().processSuper(req, player, responseMaps);
			return;
		}
	}

}
