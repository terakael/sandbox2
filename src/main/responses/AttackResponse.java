package main.responses;

import main.processing.FightManager;
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
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you're already fighting!");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		AttackRequest request = (AttackRequest)req;
		NPC npc = NPCManager.get().getNpcByInstanceId(request.getObjectId());// request tileid is the instnace id
		if (npc == null) {
			setRecoAndResponseText(0, "you can't attack that.");
			return;
		}
		
		if (FightManager.fightWithFighterExists(npc)) {
			setRecoAndResponseText(0, "someone is already fighting that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (!PathFinder.isNextTo(npc.getTileId(), player.getTileId())) {
			player.setTarget(npc);	
			player.setSavedRequest(request);
		} else {
			// start the fight
			player.setState(PlayerState.fighting);
			player.setTileId(npc.getTileId());
			FightManager.addFight(player, npc);
			
			PvmStartResponse pvmStart = new PvmStartResponse();
			pvmStart.setPlayerId(player.getId());
			pvmStart.setMonsterId(npc.getInstanceId());
			pvmStart.setTileId(npc.getTileId());
			responseMaps.addBroadcastResponse(pvmStart);
		}
		
		
	}

}
