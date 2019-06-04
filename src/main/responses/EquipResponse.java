package main.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.database.EquipmentBonusDto;
import main.database.EquipmentDao;
import main.database.EquipmentDto;
import main.database.ItemDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.requests.EquipRequest;
import main.requests.Request;

public class EquipResponse extends Response {
	private List<Integer> equippedSlots = new ArrayList<>();
	private EquipmentBonusDto bonuses = null;

	public EquipResponse() {
		setAction("equip");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (req instanceof EquipRequest) {
			EquipRequest equipReq = (EquipRequest)req;
			ItemDto item = PlayerStorageDao.getItemFromPlayerIdAndSlot(equipReq.getId(), equipReq.getSlot());		
			
			// so we have the item from the requested slot, but is it equippable?
			EquipmentDto equip = EquipmentDao.getEquipmentByItemId(item.getId());
			if (equip == null) {
				// item isn't equippable.
				setRecoAndResponseText(0, "item not equippable");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// we also handle the unequipping here too, so if it's already equipped then unequip it.
			if (EquipmentDao.isItemEquippedByItemIdAndSlot(player.getId(), item.getId(), equipReq.getSlot())) {
				EquipmentDao.clearEquippedItem(player.getId(), equipReq.getSlot());
			} else {
				// if we are already wearing this part (e.g. helmet) then unequip it before equipping the new item
				EquipmentDao.clearEquippedItemByPartId(player.getId(), equip.getPartId());
				EquipmentDao.setEquippedItem(player.getId(), equipReq.getSlot(), equip.getItemId());
			}
		}
		
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getId());
		bonuses = EquipmentDao.getEquipmentBonusesByPlayerId(player.getId());
		
		responseMaps.addClientOnlyResponse(player, this);
	}
	
}
