package main.responses;

import java.util.ArrayList;
import java.util.Collections;

import main.database.InventoryItemDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.processing.TradeManager;
import main.processing.TradeManager.Trade;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.RescindRequest;
import main.types.ItemAttributes;
import main.types.StorageTypes;

public class RescindResponse extends Response {
	public RescindResponse() {
		setAction("rescind");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof RescindRequest))
			return;
		
		Trade trade = TradeManager.getTradeWithPlayer(player);
		if (trade == null) {
			return;
		}
		
		RescindRequest request = (RescindRequest)req;
		int itemId = request.getObjectId();
		
		if (ItemDao.getItem(itemId) == null) {
			setRecoAndResponseText(0, "invalid item");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		InventoryItemDto item = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.TRADE.getValue(), request.getSlot());
		if (item == null || item.getItemId() != itemId) {
			// no message necessary tbh
			return;
		}
		
		if (ItemDao.itemHasAttribute(itemId, ItemAttributes.STACKABLE)) {
			int count = Math.min(item.getCount(), request.getAmount());
			if (count == -1)
				count = item.getCount();
			
			if (count == item.getCount()) {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.TRADE.getValue(), request.getSlot(), 0, 1);
			} else {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.TRADE.getValue(), request.getSlot(), -count);
			}
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), itemId, count);
		} else {
			ArrayList<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.TRADE.getValue());
			
			// request amount of -1 means "all"
			int count = Math.min(Collections.frequency(invItemIds, itemId), request.getAmount());
			if (count == -1)
				count = 20;// inventory size
			
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.TRADE.getValue(), request.getSlot(), 0, 1);
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), itemId, 1);
			
			invItemIds.set(request.getSlot(), -1);
			
			for (int i = count - 1; i > 0; --i) {
				int index = invItemIds.indexOf(itemId);
					if (index == -1)
						break;
					
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.TRADE.getValue(), index, 0, 1);
				invItemIds.set(index, -1);
				
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), itemId, 1);
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
