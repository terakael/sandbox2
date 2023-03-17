package responses;

import processing.PathFinder;
import processing.attackable.Attackable;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
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
		player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), walkingTargetTileId, false));
	}
	
	@Override
	public void process(Request request, Player player, ResponseMaps responseMaps) {
		if (!setTarget(request, player, responseMaps))
			return;
		
		if (!nextToTarget(request, player, responseMaps)) {
			walkToTarget(request, player, responseMaps);
			if (target != null)
				player.setTarget(target);
			
			player.setState(target == null ? PlayerState.walking : PlayerState.chasing);
			player.setSavedRequest(request);
			return;
		} else {
			player.faceDirection(target == null ? walkingTargetTileId : target.getTileId(), responseMaps);
			doAction(request, player, responseMaps);
		}
	}
	
	protected abstract void doAction(Request request, Player player, ResponseMaps responseMaps);
}
