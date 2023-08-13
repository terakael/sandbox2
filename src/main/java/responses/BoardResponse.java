package responses;

import database.dao.PlayerDao;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Ship;
import processing.managers.ShipManager;
import requests.BoardRequest;
import requests.Request;

public class BoardResponse extends WalkAndDoResponse {
	
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
		BoardRequest request = (BoardRequest)req;
		
		final Ship ship = ShipManager.getShipsAt(player.getFloor(), request.getTileId()).stream()
			.filter(e -> e.getCaptainId() == request.getObjectId())
			.findFirst()
			.orElse(null);
		
		if (ship == null)
			return;
		
		if (!ship.boardPlayer(player)) {
			setRecoAndResponseText(0, "boat's full.");
			responseMaps.addClientOnlyResponse(player, this);
		}
	}

}
