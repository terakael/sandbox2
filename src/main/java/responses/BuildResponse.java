package responses;

import processing.PathFinder;
import processing.attackable.Player;
import processing.managers.ShipManager;
import requests.BuildRequest;
import requests.Request;

public class BuildResponse extends WalkAndDoResponse {
	
	@Override
	protected boolean nextToTarget(Request request, Player player, ResponseMaps responseMaps) {
		return PathFinder.isAdjacent(player.getTileId(), walkingTargetTileId);
	}
	
	@Override
	protected void walkToTarget(Request request, Player player, ResponseMaps responseMaps) {
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), walkingTargetTileId, true));
	}

	@Override
	protected void doAction(Request req, Player player, ResponseMaps responseMaps) {
		BuildRequest request = (BuildRequest)req;
		if (request.getItemId() == null) {
			// they've clicked the boat, so show the menu
//			new ShowConstructionTableResponse(constructables, false, player.getTileId()).process(null, player, responseMaps);
			
			ShipManager.finishShip(player.getFloor(), request.getTileId(), responseMaps);
			return;
		}
		
		// they've selected an item from the menu, so check the materials
		
//		ShipManager.addShip
	}

}
