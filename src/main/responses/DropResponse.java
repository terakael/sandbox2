package main.responses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.Session;

import main.GroundItemManager;
import main.database.EquipmentDao;
import main.database.ItemDto;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerInventoryDao;
import main.processing.WorldProcessor;
import main.requests.DropRequest;
import main.requests.Request;
import main.state.Player;

public class DropResponse extends Response {
	private List<GroundItemManager.GroundItem> groundItems;
	
	public DropResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof DropRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		Player player = WorldProcessor.playerSessions.get(client);
		
		DropRequest dropReq = (DropRequest)req;
		ItemDto itemToDrop = PlayerInventoryDao.getItemFromPlayerIdAndSlot(player.getDto().getId(), dropReq.getSlot());
		if (itemToDrop == null) {
			setRecoAndResponseText(0, "you can't drop an item that doesn't exist.");
			responseMaps.addClientOnlyResponse(player, this);
			return ResponseType.client_only;
		}
		
		// check if the item is equipped
		List<Integer> equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getDto().getId());
		if (equippedSlots.contains(dropReq.getSlot())) {
			// the slot is equipped, we can't drop it
			setRecoAndResponseText(0, "you can't drop it while it's equipped.");
			responseMaps.addClientOnlyResponse(player, this);
			return ResponseType.client_only;
		}
		
		GroundItemManager.add(itemToDrop.getId(), player.getTileId());
		PlayerInventoryDao.setItemFromPlayerIdAndSlot(dropReq.getId(), dropReq.getSlot(), 0);
		
		groundItems = GroundItemManager.getGroundItems();
		
		// update the player inventory/equipped items and only send it to the player
		InventoryUpdateResponse resp = new InventoryUpdateResponse("invupdate");
		resp.setInventory(PlayerInventoryDao.getInventoryListByPlayerId(player.getDto().getId()));
		resp.setEquippedSlots(EquipmentDao.getEquippedSlotsByPlayerId(player.getDto().getId()));
		
		responseMaps.addClientOnlyResponse(player, resp);
	
		responseMaps.addBroadcastResponse(this);
		return ResponseType.broadcast;
	}

}
