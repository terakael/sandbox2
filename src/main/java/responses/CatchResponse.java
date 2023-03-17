package responses;

import database.dao.CatchableDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.managers.LocationManager;
import requests.CatchRequest;
import requests.Request;
import types.StorageTypes;

@SuppressWarnings("unused")
public class CatchResponse extends WalkAndDoResponse {
	private int instanceId;

	@Override
	protected boolean setTarget(Request request, Player player, ResponseMaps responseMaps) {
		target = LocationManager.getNpcNearPlayerByInstanceId(player, ((CatchRequest)request).getObjectId());
		if (target == null) {
			setRecoAndResponseText(0, "you can't catch that.");
			responseMaps.addClientOnlyResponse(player, this);
			return false;
		}
		return true;
	}

	@Override
	protected void doAction(Request request, Player player, ResponseMaps responseMaps) {
		// do the action once and then unset the target so we don't sit there following the target
		player.setTarget(null);
		
		final NPC npc = (NPC)target;
		if (!CatchableDao.isCatchable(npc.getId()))
			return;
		
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
