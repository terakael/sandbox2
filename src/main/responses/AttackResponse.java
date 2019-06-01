package main.responses;

import main.processing.NPC;
import main.processing.NPCManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.AttackRequest;
import main.requests.Request;

public class AttackResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof AttackRequest))
			return;
		
		AttackRequest request = (AttackRequest)req;
		NPC npc = NPCManager.get().getNpcById(request.getId());
		if (npc == null) {
			setRecoAndResponseText(0, "you can't attack that.");
			return;
		}
		
		if (!PathFinder.isNextTo(npc.getTileId(), player.getTileId())) {
			player.setState(PlayerState.chasing);	
			player.setSavedRequest(request);
		} else {
			// start the fight
			player.setState(PlayerState.fighting);
		}
		
		
	}

}
