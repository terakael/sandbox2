package main.responses;

import java.util.List;
import javax.websocket.Session;

import main.GroundItemManager;
import main.database.PlayerInventoryDao;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.processing.Player.PlayerState;
import main.requests.Request;
import main.requests.TakeRequest;

public class TakeResponse extends Response {
	@SuppressWarnings("unused")// used on serialization
	private List<GroundItemManager.GroundItem> groundItems;

	public TakeResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof TakeRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}
		
		TakeRequest takeReq = (TakeRequest)req;
		
		Player player = WorldProcessor.playerSessions.get(client);
		
		// TODO check player distance from groundItem
		GroundItemManager.GroundItem item = GroundItemManager.getGroundItemByGroundItemId(takeReq.getGroundItemId());
		if (item == null) {
			setRecoAndResponseText(0, "item doesn't exist");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		
		if (item.getTileId() != player.getTileId()) {			
			// walk over to the item before picking it up
			player.setPath(PathFinder.findPath(player.getTileId(), item.getTileId(), true));
			player.setState(PlayerState.walking);
			
			// save the request so the player reprocesses it when they arrive at their destination
			player.setSavedRequest(req);
			return;
		}
		
		if (PlayerInventoryDao.addItemByItemIdPlayerId(takeReq.getId(), item.getId()))
			GroundItemManager.remove(takeReq.getGroundItemId());
		
		groundItems = GroundItemManager.getGroundItems();
	
		// update the player inventory/equipped items and only send it to the player
		InventoryUpdateResponse resp = new InventoryUpdateResponse("invupdate");
		resp.process(takeReq, client, responseMaps);// adds itself to the appropriate responseMap
//		resp.setInventory(PlayerInventoryDao.getInventoryListByPlayerId(takeReq.getId()));
//		resp.setEquippedSlots(EquipmentDao.getEquippedSlotsByPlayerId(takeReq.getId()));
		
		//responseMaps.addClientOnlyResponse(player, resp);
	
		player.setState(PlayerState.idle);
		
		responseMaps.addBroadcastResponse(this);
	}

}
