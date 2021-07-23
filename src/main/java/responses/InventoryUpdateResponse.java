package responses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Setter;
import database.dao.EquipmentDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.ClientResourceManager;
import requests.InventoryMoveRequest;
import requests.Request;
import requests.RequestFactory;
import types.StorageTypes;

@Setter
@SuppressWarnings("unused")
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
		
		inventory = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.INVENTORY);
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getId());
		
		responseMaps.addClientOnlyResponse(player, this);
		
		// if we've never sent the item before, we need to send the corresponding sprite map
		ClientResourceManager.addItems(player, inventory.values().stream().map(InventoryItemDto::getItemId).collect(Collectors.toSet()));
	}
	
	private void processInventoryMoveRequest(InventoryMoveRequest req, Player player) {
		Map<Integer, InventoryItemDto> items = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.INVENTORY);
		InventoryItemDto destItem = items.get(req.getDest());
		InventoryItemDto srcItem = items.get(req.getSrc());

		boolean destItemEquipped = EquipmentDao.isSlotEquipped(player.getId(), req.getDest());
		boolean srcItemEquipped = EquipmentDao.isSlotEquipped(player.getId(), req.getSrc());
		
		// clear out the equipped items so we can reset them after the move
		if (destItemEquipped)
			EquipmentDao.clearEquippedItem(player.getId(), req.getDest());
		
		if (srcItemEquipped)
			EquipmentDao.clearEquippedItem(player.getId(), req.getSrc());
		
		if (destItem != null) {
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, req.getSrc(), destItem.getItemId(), destItem.getCount(), destItem.getCharges());
			if (destItemEquipped) {
				// create entry in player_equipment to update slot and item, then remove old entry
				EquipmentDao.setEquippedItem(player.getId(), req.getSrc(), destItem.getItemId());
			}
		}
		
		PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, req.getDest(), srcItem.getItemId(), srcItem.getCount(), srcItem.getCharges());
		if (srcItemEquipped) {
			EquipmentDao.setEquippedItem(player.getId(), req.getDest(), srcItem.getItemId());
		}
		
		player.recacheEquippedItems();
		
		// if the player is doing anything but walking, cancel their state.
		// if they are, for example, mid-assemble and then they move their flatpack to a different slot
		// then the finishAssemble code can't find the flatpack at the specified slot.
		if (player.getState() != PlayerState.walking)
			player.setState(PlayerState.idle);
	}

}
