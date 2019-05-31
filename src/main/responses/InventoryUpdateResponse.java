package main.responses;

import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import main.database.EquipmentDao;
import main.database.ItemDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.InventoryMoveRequest;
import main.requests.Request;

@Setter
public class InventoryUpdateResponse extends Response {	
	private List<Integer> inventory = new ArrayList<>();
	private List<Integer> equippedSlots = new ArrayList<>();

	public InventoryUpdateResponse() {
		setAction("invupdate");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {		
		if (req instanceof InventoryMoveRequest)
			processInventoryMoveRequest((InventoryMoveRequest)req, player);
		
		inventory = PlayerStorageDao.getInventoryListByPlayerId(req.getId());
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(req.getId());
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
	private void processInventoryMoveRequest(InventoryMoveRequest req, Player player) {
		// check if there's already an item in the slot we're trying to move the src item to
		ItemDto destItem = PlayerStorageDao.getItemFromPlayerIdAndSlot(req.getId(), req.getDest());
		ItemDto srcItem = PlayerStorageDao.getItemFromPlayerIdAndSlot(req.getId(), req.getSrc());
		
		boolean destItemEquipped = EquipmentDao.isSlotEquipped(req.getId(), req.getDest());
		boolean srcItemEquipped = EquipmentDao.isSlotEquipped(req.getId(), req.getSrc());
		
		// clear out the equipped items so we can reset them after the move
		if (destItemEquipped)
			EquipmentDao.clearEquippedItem(req.getId(), req.getDest());
		
		if (srcItemEquipped)
			EquipmentDao.clearEquippedItem(req.getId(), req.getSrc());
		
		if (destItem != null) {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(req.getId(), req.getSrc(), destItem.getId());
			if (destItemEquipped) {
				// create entry in player_equipment to update slot and item, then remove old entry
				EquipmentDao.setEquippedItem(req.getId(), req.getSrc(), destItem.getId());
			}
		}
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(req.getId(), req.getDest(), srcItem.getId());
		if (srcItemEquipped) {
			EquipmentDao.setEquippedItem(req.getId(), req.getDest(), srcItem.getId());
		}
	}

}
