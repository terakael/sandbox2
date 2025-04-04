package responses;

import database.dao.SceneryDao;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.WallManager;
import requests.ClimbThroughRequest;
import requests.Request;
import types.SceneryContextOptions;

public class ClimbThroughResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ClimbThroughRequest))
			return;
		
		ClimbThroughRequest request = (ClimbThroughRequest)req;
		
		// can we climb through this thing?
		final int sceneryId = WallManager.getWallSceneryIdByFloorAndTileId(player.getFloor(), request.getTileId());
		if (sceneryId == -1)
			return; // invalid scenery
		
		if (!SceneryDao.sceneryContainsContextOption(sceneryId, SceneryContextOptions.CLIMB_THROUGH))
			return; // can't climb through this

		// whatever we're climbing through has two tiles - the tileId where the scenery sits, and the tileId on the other side.
		final int impassable = WallManager.getImpassableTypeByFloorAndTileId(player.getFloor(), request.getTileId());
		
		int throughTileId = PathFinder.calculateThroughTileId(request.getTileId(), impassable);
		if (throughTileId == -1)
			return;
		
		// we want to start by walking to the closest tile to us (i.e. the side of the wall we're on)
		int closestTileId = PathFinder.getCloserTile(player.getTileId(), request.getTileId(), throughTileId);
		// if we're around a corner from the wall then the other side could be the closest tile.
		// we need to check if we can actually move to the tile.
		if (closestTileId != player.getTileId() && PathFinder.findPath(player.getFloor(), player.getTileId(), closestTileId, true).isEmpty())
			closestTileId = closestTileId == request.getTileId() ? throughTileId : request.getTileId();
		
		if (player.getTileId() != closestTileId) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), closestTileId, true));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			// our destination tile is the tile that isn't closest, i.e. the one on the other side of the wall
			final int newTileId = throughTileId == closestTileId ? request.getTileId() : throughTileId;
			player.setTileId(newTileId);
			
			PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
			playerUpdate.setId(player.getId());
			playerUpdate.setTileId(newTileId);
			responseMaps.addLocalResponse(player.getFloor(), request.getTileId(), playerUpdate);
		}
	}
	
}
