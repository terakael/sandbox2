package responses;

import java.util.Map;

import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.WorldProcessor;
import processing.attackable.Player;
import processing.managers.TradeManager;
import processing.managers.TradeManager.Trade;
import requests.Request;
import requests.RequestFactory;
import types.StorageTypes;

public class CancelTradeResponse extends Response {
	public CancelTradeResponse() {
		setAction("cancel_trade");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
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
