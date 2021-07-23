package responses;

import java.util.Set;

import database.dao.EquipmentDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import requests.DropRequest;
import requests.Request;
import requests.RequestFactory;
import system.GroundItemManager;
import types.StorageTypes;

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
		InventoryItemDto itemToDrop = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getDto().getId(), StorageTypes.INVENTORY, dropReq.getSlot());
		if (itemToDrop == null || itemToDrop.getItemId() == 0) {
			// better not to send the player a response here for obfuscation reasons I think
			return;
		}
		
		// check if the item is equipped
		Set<Integer> equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getDto().getId());
		if (equippedSlots.contains(dropReq.getSlot())) {
			// the slot is equipped, we can't drop it
			setRecoAndResponseText(0, "you can't drop it while it's equipped.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		GroundItemManager.add(player.getFloor(), player.getId(), itemToDrop.getItemId(), player.getTileId(), itemToDrop.getCount(), itemToDrop.getCharges());
		PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, dropReq.getSlot(), 0, 1, 0);
		
		// update the player inventory/equipped items and only send it to the player
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		
		// dropping should interrupt any action except walking
		// otherwise there's a potential exploit where the player can drop something mid-action and if 
		// the FinishResponse doesn't check for it then they basically can make free shit
		if (player.getState() != PlayerState.idle && player.getState() != PlayerState.walking) {
			player.setState(PlayerState.idle);
		}
	}

}
