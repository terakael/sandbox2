package responses;

import processing.attackable.Player;
import processing.attackable.Ship;
import processing.managers.ShipManager;
import requests.DepositRequest;
import requests.Request;

public class ShipDepositResponse extends StorageDepositResponse {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		DepositRequest request = (DepositRequest)req;
		
		Ship ship = ShipManager.getShipByCaptainId(player.getId());
		if (ship == null) {
			// player doesn't have a ship
			return;
		}
		
		// assume that if we got into this response then the validations have already passed
		deposit(player, ship.getStorage(), request, responseMaps);
	}

}
