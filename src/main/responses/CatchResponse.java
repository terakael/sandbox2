package main.responses;

import main.database.CatchableDao;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.processing.NPC;
import main.processing.NPCManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.CatchRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.StorageTypes;

public class CatchResponse extends Response {
	private int instanceId;

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof CatchRequest))
			return;
		
		CatchRequest request = (CatchRequest)req;
		NPC npc = NPCManager.get().getNpcByInstanceId(player.getFloor(), request.getObjectId());
		if (npc == null) {
			setRecoAndResponseText(0, "you can't catch that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (npc.isDead())
			return;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), npc.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), npc.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
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
