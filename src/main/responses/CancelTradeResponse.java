package main.responses;

import java.util.HashMap;
import java.util.Map;

import main.database.InventoryItemDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.processing.TradeManager;
import main.processing.TradeManager.Trade;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.StorageTypes;

public class CancelTradeResponse extends Response {
	public CancelTradeResponse() {
		setAction("cancel_trade");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// do the requesting player stuff first, just in case the other player logged out
		Map<Integer, InventoryItemDto> playerItems = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.TRADE);
		restoreItemsToPlayer(player, playerItems, responseMaps);
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		responseMaps.addClientOnlyResponse(player, this);
		
		// if the other player logged out then there isn't a trade anymore
		Trade trade = TradeManager.getTradeWithPlayer(player);
		if (trade == null)
			return;
		
		Player otherPlayer = trade.getOtherPlayer(player);
		Map<Integer, InventoryItemDto> otherPlayerItems = trade.getItemsByPlayer(otherPlayer);
		restoreItemsToPlayer(otherPlayer, otherPlayerItems, responseMaps);
		
		TradeManager.cancelTrade(trade);
		
		new InventoryUpdateResponse().process(RequestFactory.create("", otherPlayer.getId()), otherPlayer, responseMaps);
		
		CancelTradeResponse otherPlayerCancelResponse = new CancelTradeResponse();
		otherPlayerCancelResponse.setRecoAndResponseText(1, "other player declined trade.");
		responseMaps.addClientOnlyResponse(otherPlayer, otherPlayerCancelResponse);
	}
	
	private void restoreItemsToPlayer(Player player, Map<Integer, InventoryItemDto> items, ResponseMaps responseMaps) {
		for (InventoryItemDto dto : items.values()) {
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, dto.getItemId(), dto.getCount(), dto.getCharges());
		}
		PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(player.getId(), StorageTypes.TRADE);
	}

}
