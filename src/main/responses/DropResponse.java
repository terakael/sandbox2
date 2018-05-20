package main.responses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.websocket.Session;

import main.GroundItemManager;
import main.database.EquipmentDao;
import main.database.ItemDto;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerInventoryDao;
import main.requests.DropRequest;
import main.requests.Request;

public class DropResponse extends Response {
	private List<GroundItemManager.GroundItem> groundItems;
	
	public DropResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {
		if (!(req instanceof DropRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		DropRequest dropReq = (DropRequest)req;
		ItemDto itemToDrop = PlayerInventoryDao.getItemFromPlayerIdAndSlot(dropReq.getId(), dropReq.getSlot());
		if (itemToDrop == null) {
			setRecoAndResponseText(0, "you can't drop an item that doesn't exist.");
			return ResponseType.client_only;
		}
		
		// check if the item is equipped
		List<Integer> equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(dropReq.getId());
		if (equippedSlots.contains(dropReq.getSlot())) {
			// the slot is equipped, we can't drop it
			setRecoAndResponseText(0, "you can't drop it while it's equipped.");
			return ResponseType.client_only;
		}
		
		PlayerDto player = PlayerDao.getPlayerById(dropReq.getId());
		
		GroundItemManager.add(itemToDrop.getId(), player.getX(), player.getY());
		PlayerInventoryDao.setItemFromPlayerIdAndSlot(dropReq.getId(), dropReq.getSlot(), 0);
		
		groundItems = GroundItemManager.getGroundItems();
		
		try {
			// update the player inventory/equipped items and only send it to the player
			InventoryUpdateResponse resp = new InventoryUpdateResponse("invupdate");
			resp.setInventory(PlayerInventoryDao.getInventoryListByPlayerId(dropReq.getId()));
			resp.setEquippedSlots(EquipmentDao.getEquippedSlotsByPlayerId(dropReq.getId()));
			
			client.getBasicRemote().sendText(gson.toJson(resp));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return ResponseType.broadcast;
	}

}
