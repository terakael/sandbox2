package main.responses;

import java.util.ArrayList;
import java.util.List;

import javax.websocket.Session;

import main.database.EquipmentDao;
import main.database.EquipmentDto;
import main.database.ItemDto;
import main.database.PlayerInventoryDao;
import main.requests.EquipRequest;
import main.requests.Request;

public class EquipResponse extends Response {
	private List<Integer> equippedSlots = new ArrayList<>();

	public EquipResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {
		if (!(req instanceof EquipRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		EquipRequest equipReq = (EquipRequest)req;
		ItemDto item = PlayerInventoryDao.getItemFromPlayerIdAndSlot(equipReq.getId(), equipReq.getSlot());		
		
		// so we have the item from the requested slot, but is it equippable?
		EquipmentDto equip = EquipmentDao.getEquipmentByItemId(item.getId());
		if (equip == null) {
			// item isn't equippable.
			setRecoAndResponseText(0, "item not equippable");
			return ResponseType.client_only;
		}
		
		// we also handle the unequipping here too, so if it's already equipped then unequip it.
		if (EquipmentDao.isItemEquippedByItemIdAndSlot(equipReq.getId(), item.getId(), equipReq.getSlot())) {
			EquipmentDao.clearEquippedItem(equipReq.getId(), equipReq.getSlot());
		} else {
			// if we are already wearing this part (e.g. helmet) then unequip it before equipping the new item
			EquipmentDao.clearEquippedItemByPartId(equipReq.getId(), equip.getPartId());
			EquipmentDao.setEquippedItem(equipReq.getId(), equipReq.getSlot(), equip.getItemId());
		}
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(equipReq.getId());
		
		return ResponseType.client_only;// will probs change to broadcast when we can see their worn items
	}
	
}
