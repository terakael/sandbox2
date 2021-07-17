package main.responses;

import main.database.dao.CatchableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.processing.PathFinder;
import main.processing.WorldProcessor;
import main.processing.attackable.NPC;
import main.processing.attackable.Player;
import main.processing.attackable.Player.PlayerState;
import main.processing.managers.FightManager;
import main.processing.managers.NPCManager;
import main.requests.CatchRequest;
import main.requests.Request;
import main.types.StorageTypes;

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
