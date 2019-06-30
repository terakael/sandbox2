package main.responses;

import java.util.ArrayList;
import java.util.HashMap;

import main.database.InventoryItemDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.processing.TradeManager;
import main.processing.TradeManager.Trade;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.ItemAttributes;
import main.types.StorageTypes;

public class CancelTradeResponse extends Response {
	public CancelTradeResponse() {
		setAction("cancel_trade");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// do the requesting player stuff first, just in case the other player logged out
		HashMap<Integer, InventoryItemDto> playerItems = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.TRADE.getValue());
		restoreItemsToPlayer(player, playerItems, responseMaps);
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		responseMaps.addClientOnlyResponse(player, this);
		
		// if the other player logged out then there isn't a trade anymore
		Trade trade = TradeManager.getTradeWithPlayer(player);
		if (trade == null)
			return;
		
		Player otherPlayer = trade.getOtherPlayer(player);
		HashMap<Integer, InventoryItemDto> otherPlayerItems = trade.getItemsByPlayer(otherPlayer);
		restoreItemsToPlayer(otherPlayer, otherPlayerItems, responseMaps);
		
		TradeManager.cancelTrade(trade);
		
		new InventoryUpdateResponse().process(RequestFactory.create("", otherPlayer.getId()), otherPlayer, responseMaps);
		
		CancelTradeResponse otherPlayerCancelResponse = new CancelTradeResponse();
		otherPlayerCancelResponse.setRecoAndResponseText(1, "other player declined trade.");
		responseMaps.addClientOnlyResponse(otherPlayer, otherPlayerCancelResponse);
	}
	
	private void restoreItemsToPlayer(Player player, HashMap<Integer, InventoryItemDto> items, ResponseMaps responseMaps) {
		for (InventoryItemDto dto : items.values()) {
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), dto.getItemId(), dto.getCount());
		}
		PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(player.getId(), StorageTypes.TRADE.getValue());
	}

}
