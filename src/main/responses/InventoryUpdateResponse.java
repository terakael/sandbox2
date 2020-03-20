package main.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import lombok.Setter;
import main.database.EquipmentDao;
import main.database.InventoryItemDto;
import main.database.ItemDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.InventoryMoveRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.StorageTypes;

@Setter
public class InventoryUpdateResponse extends Response {	
	private HashMap<Integer, InventoryItemDto> inventory = new HashMap<>();
	private HashSet<Integer> equippedSlots = new HashSet<>();

	public InventoryUpdateResponse() {
		setAction("invupdate");
	}
	
	public static void sendUpdate(Player player, ResponseMaps responseMaps) {
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {		
		if (req instanceof InventoryMoveRequest)
			processInventoryMoveRequest((InventoryMoveRequest)req, player);
		
		inventory = PlayerStorageDao.getStorageDtoMapByPlayerId(req.getId(), StorageTypes.INVENTORY.getValue());
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(req.getId());
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
	private void processInventoryMoveRequest(InventoryMoveRequest req, Player player) {
		HashMap<Integer, InventoryItemDto> items = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
		InventoryItemDto destItem = items.get(req.getDest());
		InventoryItemDto srcItem = items.get(req.getSrc());

		boolean destItemEquipped = EquipmentDao.isSlotEquipped(req.getId(), req.getDest());
		boolean srcItemEquipped = EquipmentDao.isSlotEquipped(req.getId(), req.getSrc());
		
		// clear out the equipped items so we can reset them after the move
		if (destItemEquipped)
			EquipmentDao.clearEquippedItem(req.getId(), req.getDest());
		
		if (srcItemEquipped)
			EquipmentDao.clearEquippedItem(req.getId(), req.getSrc());
		
		if (destItem != null) {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(req.getId(), StorageTypes.INVENTORY.getValue(), req.getSrc(), destItem.getItemId(), destItem.getCount(), destItem.getCharges());
			if (destItemEquipped) {
				// create entry in player_equipment to update slot and item, then remove old entry
				EquipmentDao.setEquippedItem(req.getId(), req.getSrc(), destItem.getItemId());
			}
		}
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(req.getId(), StorageTypes.INVENTORY.getValue(), req.getDest(), srcItem.getItemId(), srcItem.getCount(), srcItem.getCharges());
		if (srcItemEquipped) {
			EquipmentDao.setEquippedItem(req.getId(), req.getDest(), srcItem.getItemId());
		}
		
		player.recacheEquippedItems();
	}

}
