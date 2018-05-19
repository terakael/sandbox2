package main.responses;

import java.util.ArrayList;
import java.util.List;

import javax.websocket.Session;

import lombok.AllArgsConstructor;
import lombok.Setter;
import main.database.EquipmentDao;
import main.database.ItemDto;
import main.database.PlayerInventoryDao;
import main.requests.InventoryMoveRequest;
import main.requests.InventoryUpdateRequest;
import main.requests.Request;

@Setter
public class InventoryUpdateResponse extends Response {	
	private List<Integer> inventory = new ArrayList<>();
	private List<Integer> equippedSlots = new ArrayList<>();

	public InventoryUpdateResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {		
		if (req instanceof InventoryMoveRequest)
			processInventoryMoveRequest((InventoryMoveRequest)req, client);
		
		inventory = PlayerInventoryDao.getInventoryListByPlayerId(req.getId());
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(req.getId());
		
		return ResponseType.client_only;
	}
	
	private void processInventoryMoveRequest(InventoryMoveRequest req, Session client) {
		// check if there's already an item in the slot we're trying to move the src item to
		ItemDto destItem = PlayerInventoryDao.getItemFromPlayerIdAndSlot(req.getId(), req.getDest());
		ItemDto srcItem = PlayerInventoryDao.getItemFromPlayerIdAndSlot(req.getId(), req.getSrc());
		
		boolean destItemEquipped = EquipmentDao.isSlotEquipped(req.getId(), req.getDest());
		boolean srcItemEquipped = EquipmentDao.isSlotEquipped(req.getId(), req.getSrc());
		
		// clear out the equipped items so we can reset them after the move
		if (destItemEquipped)
			EquipmentDao.clearEquippedItem(req.getId(), req.getDest());
		
		if (srcItemEquipped)
			EquipmentDao.clearEquippedItem(req.getId(), req.getSrc());
		
		if (destItem != null) {
			PlayerInventoryDao.setItemFromPlayerIdAndSlot(req.getId(), req.getSrc(), destItem.getId());
			if (destItemEquipped) {
				// create entry in player_equipment to update slot and item, then remove old entry
				EquipmentDao.setEquippedItem(req.getId(), req.getSrc(), destItem.getId());
			}
		}
		
		PlayerInventoryDao.setItemFromPlayerIdAndSlot(req.getId(), req.getDest(), srcItem.getId());
		if (srcItemEquipped) {
			EquipmentDao.setEquippedItem(req.getId(), req.getDest(), srcItem.getId());
		}
	}

}
