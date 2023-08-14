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
		return PathFinder.isAdjacent(player.getTileId(), walkingTargetTileId) || player.getTileId() == walkingTargetTileId;
	}
	
	@Override
	protected void walkToTarget(Request request, Player player, ResponseMaps responseMaps) {
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), walkingTargetTileId, true));
	}

	@Override
	protected void doAction(Request req, Player player, ResponseMaps responseMaps) {
		BoardRequest request = (BoardRequest)req;
		
		// if we're already onboard a ship, then get off at the nearest land
		final Ship boardedShip = ShipManager.getShipWithPlayer(player);
		if (boardedShip != null) {
			int closestLandTile = PathFinder.getClosestWalkableTile(boardedShip.getFloor(), boardedShip.getTileId());
			if (closestLandTile != -1) {
				boardedShip.disembarkPlayer(player);
				player.setTileId(closestLandTile);
				
				PlayerUpdateResponse disembarkResponse = new PlayerUpdateResponse();
				disembarkResponse.setId(player.getId());
				disembarkResponse.setBoardedShip(false);
				disembarkResponse.setTileId(closestLandTile);
				responseMaps.addClientOnlyResponse(player, disembarkResponse);
			}
			return;
		}
		
		final Ship ship = ShipManager.getShipsAt(player.getFloor(), request.getTileId()).stream()
			.filter(e -> e.getCaptainId() == request.getObjectId())
			.findFirst()
			.orElse(null);
		
		if (ship == null)
			return;
		
		if (!ship.boardPlayer(player)) {
			setRecoAndResponseText(0, "boat's full.");
			responseMaps.addClientOnlyResponse(player, this);
		} else {
			// send a boardedShip update to the local player.
			// other players boarding a ship will get a playerOutOfRange response
			PlayerUpdateResponse onboardResponse = new PlayerUpdateResponse();
			onboardResponse.setId(player.getId());
			onboardResponse.setBoardedShip(true);
			onboardResponse.setTileId(ship.getTileId());
			responseMaps.addClientOnlyResponse(player, onboardResponse);
		}
	}

}
