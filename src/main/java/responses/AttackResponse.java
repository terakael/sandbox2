package responses;

import database.dao.NPCDao;
import processing.PathFinder;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.attackable.Ship;
import processing.managers.FightManager;
import processing.managers.LocationManager;
import processing.managers.ShipManager;
import requests.AttackRequest;
import requests.Request;
import types.NpcAttributes;

public class AttackResponse extends WalkAndDoResponse {
	private transient Ship ship = null;
	
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
		
		// i think any player aboard can fire the cannons?  good for co-op
		ship = ShipManager.getShipWithPlayer(player);
		
		return true;
	}
	
	@Override
	protected boolean nextToTarget(Request request, Player player, ResponseMaps responseMaps) {
		// if we're attacking from a ship, the ship won't move automatically towards the target.
		// the captain needs to manually get as close as possible and then someone will
		// need to attack the target.  Closer to the target, more accurate the cannon.
		// however if they're on the same tile then move the ship away a bit
		if (ship != null && ship.getTileId() != target.getTileId())
			return true;
		return super.nextToTarget(request, player, responseMaps);
	}

	@Override
	protected void doAction(Request request, Player player, ResponseMaps responseMaps) {		
		if (ship != null) {
			final String res = ship.verifyFireCannon(target);
			if (!res.isEmpty()) {
				setRecoAndResponseText(0, res);
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			ship.setTarget(target);
			player.setState(PlayerState.charging_cannon);
			
			setRecoAndResponseText(0, "you start loading the cannon...");
			responseMaps.addClientOnlyResponse(player, this);
			
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
