package main.responses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Setter;
import main.database.EquipmentDao;
import main.database.InventoryItemDto;
import main.database.PlayerStorageDao;
import main.processing.ClientResourceManager;
import main.processing.Player;
import main.requests.InventoryMoveRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.StorageTypes;

@Setter
public class InventoryUpdateResponse extends Response {	
	private Map<Integer, InventoryItemDto> inventory = new HashMap<>();
	private Set<Integer> equippedSlots = new HashSet<>();

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
		
		inventory = PlayerStorageDao.getStorageDtoMapByPlayerId(req.getId(), StorageTypes.INVENTORY);
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(req.getId());
		
		responseMaps.addClientOnlyResponse(player, this);
		
		// if we've never sent the item before, we need to send the corresponding sprite map
		ClientResourceManager.addItems(player, inventory.values().stream().map(InventoryItemDto::getItemId).collect(Collectors.toSet()));
	}
	
	private void processInventoryMoveRequest(InventoryMoveRequest req, Player player) {
		Map<Integer, InventoryItemDto> items = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.INVENTORY);
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
			PlayerStorageDao.setItemFromPlayerIdAndSlot(req.getId(), StorageTypes.INVENTORY, req.getSrc(), destItem.getItemId(), destItem.getCount(), destItem.getCharges());
			if (destItemEquipped) {
				// create entry in player_equipment to update slot and item, then remove old entry
				EquipmentDao.setEquippedItem(req.getId(), req.getSrc(), destItem.getItemId());
			}
		}
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(req.getId(), StorageTypes.INVENTORY, req.getDest(), srcItem.getItemId(), srcItem.getCount(), srcItem.getCharges());
		if (srcItemEquipped) {
			EquipmentDao.setEquippedItem(req.getId(), req.getDest(), srcItem.getItemId());
		}
		
		player.recacheEquippedItems();
	}

}
