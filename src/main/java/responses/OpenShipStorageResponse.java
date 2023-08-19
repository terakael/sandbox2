package responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import database.dto.InventoryItemDto;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Ship;
import processing.managers.ShipManager;
import requests.OpenShipStorageRequest;
import requests.Request;

public class OpenShipStorageResponse extends WalkAndDoResponse {
	private List<InventoryItemDto> items = null;
	private String name = "ship storage";
	private int tileId;
	
	protected boolean setTarget(Request req, Player player, ResponseMaps responseMaps) {
		OpenShipStorageRequest request = (OpenShipStorageRequest)req;
		final Ship ship = ShipManager.getShipByCaptainId(request.getObjectId());
		if (ship == null) {
			// ship doesn't exist.
			return false;
		}
		
		target = ship;
		return true;
	}
	
	@Override
	protected boolean nextToTarget(Request request, Player player, ResponseMaps responseMaps) {
		return PathFinder.isAdjacent(player.getTileId(), target.getTileId()) || ShipManager.getShipWithPlayer(player) == target;
	}
	
	@Override
	protected void walkToTarget(Request request, Player player, ResponseMaps responseMaps) {
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), target.getTileId(), true));
	}

	@Override
	protected void doAction(Request req, Player player, ResponseMaps responseMaps) {
		final Ship ship = (Ship)target;
		
		if (ship.getCaptainId() != player.getId()) {
			setRecoAndResponseText(0, "the captain wouldn't want you snooping in there.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		items = ship.getStorage().getItems();
		tileId = ship.getTileId();
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
