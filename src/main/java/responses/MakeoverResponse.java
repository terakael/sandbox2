package responses;

import processing.attackable.NPC;
import processing.attackable.Player;
import processing.managers.LocationManager;
import requests.MakeoverRequest;
import requests.Request;

public class MakeoverResponse extends WalkAndDoResponse {
	private static final transient int clothildaNpcId = 59;

	protected boolean setTarget(Request request, Player player, ResponseMaps responseMaps) {
		target = LocationManager.getNpcNearPlayerByInstanceId(player, ((MakeoverRequest)request).getObjectId());
		if (target == null)
			return false;
		
		if (((NPC)target).getId() != clothildaNpcId)
			return false;
		
		return true;
	}

	@Override
	protected void doAction(Request request, Player player, ResponseMaps responseMaps) {
		new ShowBaseAnimationsWindowResponse().process(null, player, responseMaps);
		player.setTarget(null);
	}

}
