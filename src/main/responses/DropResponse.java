package main.responses;

import java.util.HashSet;
import java.util.List;
import lombok.Setter;
import main.FightManager;
import main.GroundItemManager;
import main.database.EquipmentDao;
import main.database.ItemDao;
import main.database.ItemDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.DropRequest;
import main.requests.Request;
import main.types.ItemAttributes;

public class DropResponse extends Response {	
	public DropResponse() {
		setAction("drop");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof DropRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't drop anything during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		DropRequest dropReq = (DropRequest)req;
		ItemDto itemToDrop = PlayerStorageDao.getItemFromPlayerIdAndSlot(player.getDto().getId(), dropReq.getSlot());
		if (itemToDrop == null) {
			setRecoAndResponseText(0, "you can't drop an item that doesn't exist.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// check if the item is equipped
		HashSet<Integer> equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getDto().getId());
		if (equippedSlots.contains(dropReq.getSlot())) {
			// the slot is equipped, we can't drop it
			setRecoAndResponseText(0, "you can't drop it while it's equipped.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		

		GroundItemManager.add(player.getId(), itemToDrop.getId(), player.getTileId());
//		groundItems = GroundItemManager.getGlobalGroundItems();
//		responseMaps.addBroadcastResponse(this);

		PlayerStorageDao.setItemFromPlayerIdAndSlot(dropReq.getId(), dropReq.getSlot(), 0);
		
		// update the player inventory/equipped items and only send it to the player
		InventoryUpdateResponse resp = (InventoryUpdateResponse)ResponseFactory.create("invupdate");
		resp.setInventory(PlayerStorageDao.getInventoryListByPlayerId(player.getDto().getId()));
		resp.setEquippedSlots(EquipmentDao.getEquippedSlotsByPlayerId(player.getDto().getId()));
		responseMaps.addClientOnlyResponse(player, resp);	
	}

}
