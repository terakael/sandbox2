package responses;

import processing.PathFinder;
import processing.attackable.Attackable;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.attackable.Ship;
import processing.managers.ShipManager;
import requests.Request;

public abstract class WalkAndDoResponse extends Response {
	protected transient int walkingTargetTileId = -1; // if the target is stationary
	protected transient Attackable target = null; // if the target is an npc
	
	protected boolean setTarget(Request request, Player player, ResponseMaps responseMaps) {
		walkingTargetTileId = request.getTileId();
		return true;
	}
	
	protected boolean nextToTarget(Request request, Player player, ResponseMaps responseMaps) {
		return PathFinder.isNextTo(player.getFloor(), player.getTileId(), target == null ? walkingTargetTileId : target.getTileId());
	}
	
	protected void walkToTarget(Request request, Player player, ResponseMaps responseMaps) {
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), target == null ? walkingTargetTileId : target.getTileId(), false));
	}
	
	protected void handleShipPassenger(Request request, Player player, ResponseMaps responseMaps) {
		
	}
	
	@Override
	public void process(Request request, Player player, ResponseMaps responseMaps) {
		final Ship ship = ShipManager.getShipWithPlayer(player);
		if (ship != null && ship.getCaptainId() != player.getId()) {
			handleShipPassenger(request, player, responseMaps);
			return; // if we're on a ship but not the captain then we can't do WalkAndDoResponses
		}
		
		if (!setTarget(request, player, responseMaps))
			return;
		
		if (!nextToTarget(request, player, responseMaps)) {
			walkToTarget(request, player, responseMaps);
			if (target != null)
				player.setTarget(target);
			
			// behaviour changes slightly when on a ship
			final PlayerState state = target != null 
					? PlayerState.chasing 
					: ship != null ? PlayerState.moving_ship : PlayerState.walking;
			
			player.setState(state);
			
			if (ship != null) {
				ship.setSavedRequest(request);
			} else {
				player.setSavedRequest(request);
			}
			return;
		} else {
			player.faceDirection(target == null ? walkingTargetTileId : target.getTileId(), responseMaps);
			doAction(request, player, responseMaps);
		}
	}
	
	protected abstract void doAction(Request request, Player player, ResponseMaps responseMaps);
}
