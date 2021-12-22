package responses;

import processing.PathFinder;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.managers.FightManager;
import processing.managers.LocationManager;
import requests.MakeoverRequest;
import requests.Request;

public class MakeoverResponse extends Response {
	private static final int clothildaNpcId = 59;

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof MakeoverRequest))
			return;
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you're too busy fighting!");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// need to be near the makeover girl to trigger it
		final NPC makeoverNpc = LocationManager.getNpcNearPlayerByInstanceId(player, ((MakeoverRequest)req).getObjectId());
		if (makeoverNpc == null)
			return;
		
		if (makeoverNpc.getId() != clothildaNpcId)
			return;
		
		if (!PathFinder.isNextTo(player.getFloor(), makeoverNpc.getTileId(), player.getTileId())) {
			player.setTarget(makeoverNpc);	
			player.setSavedRequest(req);
		} else {
			player.faceDirection(makeoverNpc.getTileId(), responseMaps);
			new ShowBaseAnimationsWindowResponse().process(null, player, responseMaps);
		}
	}

}
