package main.responses;

import java.util.Map;

import main.database.dao.PlayerStorageDao;
import main.database.dto.InventoryItemDto;
import main.processing.Player;
import main.processing.TradeManager;
import main.processing.TradeManager.Trade;
import main.processing.WorldProcessor;
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
		
		TradeManager.cancelTrade(trade);
		
		Player otherPlayer = trade.getOtherPlayer(player);
		Map<Integer, InventoryItemDto> otherPlayerItems = trade.getItemsByPlayer(otherPlayer);
		restoreItemsToPlayer(otherPlayer, otherPlayerItems, responseMaps);

		// sometimes the cancel request happened due to a disconnect.
		// if this is the case, don't update the player inventory, as this triggers the ClientResourceManager
		// to recache the item data, which means the player won't get sent it next time, causing bugs.
		if (WorldProcessor.sessionExistsByPlayerId(otherPlayer.getId())) {
			new InventoryUpdateResponse().process(RequestFactory.create("", otherPlayer.getId()), otherPlayer, responseMaps);
			
			CancelTradeResponse otherPlayerCancelResponse = new CancelTradeResponse();
			otherPlayerCancelResponse.setRecoAndResponseText(1, "other player declined trade.");
			responseMaps.addClientOnlyResponse(otherPlayer, otherPlayerCancelResponse);
		}
	}
	
	private void restoreItemsToPlayer(Player player, Map<Integer, InventoryItemDto> items, ResponseMaps responseMaps) {
		for (InventoryItemDto dto : items.values()) {
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, dto.getItemId(), dto.getCount(), dto.getCharges());
		}
		PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(player.getId(), StorageTypes.TRADE);
	}

}
