package responses;

import database.dao.CatchableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import processing.PathFinder;
import processing.WorldProcessor;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import processing.managers.NPCManager;
import requests.CatchRequest;
import requests.Request;
import types.StorageTypes;

@SuppressWarnings("unused")
public class CatchResponse extends Response {
	private int instanceId;

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof CatchRequest))
			return;
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		CatchRequest request = (CatchRequest)req;
		NPC npc = NPCManager.get().getNpcByInstanceId(player.getFloor(), request.getObjectId());
		if (npc == null) {
			setRecoAndResponseText(0, "you can't catch that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (npc.isDead() || (WorldProcessor.isDaytime() && !npc.isDiurnal()) || (!WorldProcessor.isDaytime() && !npc.isNocturnal()))
			return;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), npc.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), npc.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.faceDirection(npc.getTileId(), responseMaps);
			
			if (!CatchableDao.isCatchable(npc.getId())) {
				setRecoAndResponseText(0, "you can't catch that.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			int freeSlot = PlayerStorageDao.getFreeSlotByPlayerId(player.getId());
			if (freeSlot == -1) {
				setRecoAndResponseText(0, "your inventory is full.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			int caughtItemId = CatchableDao.getCaughtItem(npc.getId());
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, freeSlot, caughtItemId, 1, ItemDao.getMaxCharges(caughtItemId));
			
			npc.onDeath(player, responseMaps);
			instanceId = npc.getInstanceId();
			
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
			
			responseMaps.addLocalResponse(player.getFloor(), npc.getTileId(), this);
		}
	}
	
}
