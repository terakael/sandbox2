package responses;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import database.dao.EquipmentDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import processing.managers.TradeManager;
import processing.managers.TradeManager.Trade;
import requests.OfferRequest;
import requests.Request;
import requests.RequestFactory;
import types.ItemAttributes;
import types.StorageTypes;

public class OfferResponse extends Response {
	public OfferResponse() {
		setAction("offer");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof OfferRequest))
			return;
		
		Trade trade = TradeManager.getTradeWithPlayer(player);
		if (trade == null) {
			return;
		}
		
		OfferRequest request = (OfferRequest)req;
		int itemId = request.getObjectId();
		
		if (ItemDao.getItem(itemId) == null) {
			setRecoAndResponseText(0, "invalid item");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		InventoryItemDto item = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot());
		if (item == null || item.getItemId() != itemId || item.getItemId() == 0) {
			// no message necessary tbh
			System.out.println("item null");
			return;
		}
		
		Set<Integer> equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(player.getId());
		if (equippedSlots.contains(request.getSlot())) {
			setRecoAndResponseText(0, "you need to unequip it first.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// you can't trade untradeable or unique items
		if (!ItemDao.itemHasAttribute(item.getItemId(), ItemAttributes.TRADEABLE) ||
				ItemDao.itemHasAttribute(item.getItemId(), ItemAttributes.UNIQUE)) {
			setRecoAndResponseText(0, "you can't trade that.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// if the potential receiver has never loaded the item before, load it now
		ClientResourceManager.addItems(trade.getOtherPlayer(player), Collections.singleton(item.getItemId()));
		
		if (ItemDao.itemHasAttribute(itemId, ItemAttributes.STACKABLE)) {
			int count = Math.min(item.getCount(), request.getAmount());
			if (count == -1)
				count = item.getCount();// -1 means all
			
			if (count == item.getCount()) {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), 0, 1, 0);
			} else {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), -count);
			}
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.TRADE, itemId, count, item.getCharges());
		} else {
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			
			// get rid of equipped items so they don't count
			for (int i : equippedSlots)
				invItemIds.set(i, -1);
			
			int count = Math.min(Collections.frequency(invItemIds, itemId), request.getAmount());
			if (count == -1)
				count = 20;// max inventory size
			
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, request.getSlot(), 0, 1, 0);
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.TRADE, itemId, 1, item.getCharges());
			
			invItemIds.set(request.getSlot(), -1);
			
			for (int i = count - 1; i > 0; --i) {
				int index = invItemIds.indexOf(itemId);
					if (index == -1)
						break;
					
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, index, 0, 1, 0);
				invItemIds.set(index, -1);
				
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.TRADE, itemId, 1, item.getCharges());
			}
		}
		
		trade.cancelAccepts();
		
		Player otherPlayer = trade.getOtherPlayer(player);
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		new InventoryUpdateResponse().process(RequestFactory.create("", otherPlayer.getId()), otherPlayer, responseMaps);
		
		new TradeUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		new TradeUpdateResponse().process(RequestFactory.create("", otherPlayer.getId()), otherPlayer, responseMaps);
	}

}
