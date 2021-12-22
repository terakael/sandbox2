package responses;

import database.dao.NPCDao;
import processing.PathFinder;
import processing.WorldProcessor;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import processing.managers.LocationManager;
import requests.AttackRequest;
import requests.Request;
import types.NpcAttributes;

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
		final NPC npc = LocationManager.getNpcNearPlayerByInstanceId(player, request.getObjectId());
		if (npc == null || !NPCDao.npcHasAttribute(npc.getId(), NpcAttributes.ATTACKABLE)) {
			setRecoAndResponseText(0, "you can't attack that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (npc.isDead() || (WorldProcessor.isDaytime() && !npc.isDiurnal()) || (!WorldProcessor.isDaytime() && !npc.isNocturnal()))
			return;
		
		if (FightManager.fightWithFighterExists(npc)) {
			setRecoAndResponseText(0, "someone is already fighting that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (!PathFinder.isNextTo(player.getFloor(), npc.getTileId(), player.getTileId())) {
			player.setTarget(npc);	
			player.setSavedRequest(request);
		} else {
			// start the fight
			player.setState(PlayerState.fighting);
			player.setTileId(npc.getTileId());
			npc.clearPath();
			player.clearPath();
			FightManager.addFight(player, npc, true);
			
			PvmStartResponse pvmStart = new PvmStartResponse();
			pvmStart.setPlayerId(player.getId());
			pvmStart.setMonsterId(npc.getInstanceId());
			pvmStart.setTileId(npc.getTileId());
			responseMaps.addBroadcastResponse(pvmStart);
		}
		
		
	}

}
