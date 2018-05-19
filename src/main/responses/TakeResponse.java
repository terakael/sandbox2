package main.responses;

import java.io.IOException;
import java.util.List;

import javax.websocket.Session;

import main.GroundItemManager;
import main.database.EquipmentDao;
import main.database.PlayerInventoryDao;
import main.requests.Request;
import main.requests.TakeRequest;

public class TakeResponse extends Response {
	private List<GroundItemManager.GroundItem> groundItems;

	public TakeResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {
		if (!(req instanceof TakeRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		TakeRequest takeReq = (TakeRequest)req;
		
		// TODO check player distance from groundItem
		GroundItemManager.GroundItem item = GroundItemManager.getGroundItemByGroundItemId(takeReq.getGroundItemId());
		if (item == null) {
			setRecoAndResponseText(0, "item doesn't exist");
			return ResponseType.client_only;
		}
		
		if (PlayerInventoryDao.addItemByItemIdPlayerId(takeReq.getId(), item.getId()))
			GroundItemManager.remove(takeReq.getGroundItemId());
		
		groundItems = GroundItemManager.getGroundItems();
		
		try {
			// update the player inventory/equipped items and only send it to the player
			InventoryUpdateResponse resp = new InventoryUpdateResponse("invupdate");
			resp.setInventory(PlayerInventoryDao.getInventoryListByPlayerId(takeReq.getId()));
			resp.setEquippedSlots(EquipmentDao.getEquippedSlotsByPlayerId(takeReq.getId()));
			
			client.getBasicRemote().sendText(gson.toJson(resp));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ResponseType.broadcast;
	}

}
