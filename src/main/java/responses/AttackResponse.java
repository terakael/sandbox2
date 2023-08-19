package responses;

import database.dao.NPCDao;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import processing.managers.LocationManager;
import processing.managers.ShipManager;
import requests.AttackRequest;
import requests.Request;
import types.NpcAttributes;

public class AttackResponse extends WalkAndDoResponse {
	
	public AttackResponse() {
		setCombatLockedMessage("you're already fighting!");
		setCombatInterrupt(false); // even after the combat lock expires, don't allow hopping to a new fight
	}

	@Override
	protected boolean setTarget(Request request, Player player, ResponseMaps responseMaps) {
		target = LocationManager.getNpcNearPlayerByInstanceId(player, ((AttackRequest)request).getObjectId());
		if (target == null || !NPCDao.npcHasAttribute(((NPC)target).getId(), NpcAttributes.ATTACKABLE)) {
			setRecoAndResponseText(0, "you can't attack that.");
			responseMaps.addClientOnlyResponse(player, this);
			return false;
		}
		
		return true;
	}

	@Override
	protected void doAction(Request request, Player player, ResponseMaps responseMaps) {
		if (ShipManager.getShipWithPlayer(player) != null) {
			// if we're on a boat then don't start a fight
			// (TODO ship cannon)
			return;
		}
		
		NPC npc = (NPC)target;
		
		if (FightManager.fightWithFighterExists(npc)) {
			setRecoAndResponseText(0, "someone is already fighting that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
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
