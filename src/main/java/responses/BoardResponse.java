package responses;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import database.dao.ShipAccessoryDao;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Ship;
import processing.managers.ClientResourceManager;
import processing.managers.OceanFishingManager;
import processing.managers.ShipManager;
import requests.BoardRequest;
import requests.Request;

public class BoardResponse extends WalkAndDoResponse {
	
	private transient Ship ship = null;
	
	@Override
	protected boolean setTarget(Request req, Player player, ResponseMaps responseMaps) {
		ship = ShipManager.getShipByCaptainId(((BoardRequest)req).getObjectId());
		if (ship != null) {
			walkingTargetTileId = PathFinder.getClosestWalkableTile(ship.getFloor(), ship.getTileId());
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void handleShipPassenger(Request request, Player player, ResponseMaps responseMaps) {
		handleDisembark((BoardRequest)request, player, responseMaps);
	}
	
	@Override
	protected boolean nextToTarget(Request request, Player player, ResponseMaps responseMaps) {
		if (ship.playerIsAboard(player.getId()))
			return PathFinder.isAdjacent(ship.getTileId(), walkingTargetTileId);
		return PathFinder.isNextTo(ship.getFloor(), player.getTileId(), walkingTargetTileId);
	}
	
	@Override
	protected void walkToTarget(Request request, Player player, ResponseMaps responseMaps) {
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), walkingTargetTileId, true));
	}

	@Override
	protected void doAction(Request req, Player player, ResponseMaps responseMaps) {
		BoardRequest request = (BoardRequest)req;
		
		if (handleDisembark(request, player, responseMaps))
			return;
		
		// we're checking nextToTarget() as "next to the closest walkable tile to the ship".
		// but actually, if the ship is away from the land, we should return out.
		if (!PathFinder.isAdjacent(player.getTileId(), ship.getTileId()))
			return;
		
		if (!ship.boardPlayer(player, responseMaps)) {
			setRecoAndResponseText(0, "boat's full.");
			responseMaps.addClientOnlyResponse(player, this);
		}
	}
	
	private boolean handleDisembark(BoardRequest request, Player player, ResponseMaps responseMaps) {
		// if we're already onboard a ship, then get off at the nearest land
		final Ship boardedShip = ShipManager.getShipWithPlayer(player);
		if (boardedShip == null || boardedShip.getCaptainId() != request.getObjectId())
			return false; // we're not on a ship or the request's objectId doesn't match
		
		int closestLandTile = PathFinder.getClosestWalkableTile(boardedShip.getFloor(), boardedShip.getTileId());
		if (closestLandTile != -1) {
			boardedShip.disembarkPlayer(player, responseMaps);
			player.setTileId(closestLandTile);
			
			PlayerUpdateResponse disembarkResponse = new PlayerUpdateResponse();
			disembarkResponse.setId(player.getId());
			disembarkResponse.setBoardedShipId(0);
			disembarkResponse.setTileId(closestLandTile);
			responseMaps.addClientOnlyResponse(player, disembarkResponse);
		}
		return true;
	}

}
