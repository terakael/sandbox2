package main.responses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import main.database.InventoryItemDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.processing.TradeManager;
import main.processing.TradeManager.Trade;
import main.processing.WorldProcessor;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.StorageTypes;

public class AcceptTradeOfferResponse extends Response {
	HashSet<Integer> acceptedPlayerIds = null;
	public AcceptTradeOfferResponse() {
		setAction("accept_trade_offer");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		Trade trade = TradeManager.getTradeWithPlayer(player);
		if (trade == null)
			return;
		
		trade.playerAcceptsTrade(player);
		if (!trade.bothPlayersAccepted()) {
			acceptedPlayerIds = trade.getAcceptedPlayerIds();
			responseMaps.addClientOnlyResponse(player, this);
			responseMaps.addClientOnlyResponse(trade.getOtherPlayer(player), this);
			return;
		}
		
		// both players have accepted, do the trade
		Player otherPlayer = trade.getOtherPlayer(player);
		
		// what if one of the players logs out as the other one sends the final accept?
		if (!WorldProcessor.playerSessions.containsKey(player.getSession()))
			return;
		if (!WorldProcessor.playerSessions.containsKey(otherPlayer.getSession()))
			return;
		
		ArrayList<Integer> p1invItems = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
		ArrayList<Integer> p2invItems = PlayerStorageDao.getStorageListByPlayerId(otherPlayer.getId(), StorageTypes.INVENTORY.getValue());
		
		HashMap<Integer, InventoryItemDto> p1tradeItems = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.TRADE.getValue());
		HashMap<Integer, InventoryItemDto> p2tradeItems = PlayerStorageDao.getStorageDtoMapByPlayerId(otherPlayer.getId(), StorageTypes.TRADE.getValue());
		
		// TODO this doesn't count stackables.
		// eg. if player has full inventory, including coins, and another player wants to give more coins
		// this would falsely fail the check.
		if (Collections.frequency(p1invItems, 0) < (Collections.frequency(p2invItems, 0)) - p2tradeItems.size()) {
			// player 1 doesn't have enough space
			CancelTradeResponse cancelTradePlayer2 = new CancelTradeResponse();
			cancelTradePlayer2.setRecoAndResponseText(0, String.format("%s doesn't have enough space.", player.getDto().getName()));
			responseMaps.addClientOnlyResponse(otherPlayer, cancelTradePlayer2);
			
			CancelTradeResponse cancelTradePlayer1 = new CancelTradeResponse();
			cancelTradePlayer1.setRecoAndResponseText(0, "you don't have enough space.");
			responseMaps.addClientOnlyResponse(player, cancelTradePlayer1);
			return;
		}
		
		if (Collections.frequency(p2invItems,  0) < (Collections.frequency(p1invItems, 0) - p1tradeItems.size())) {
			// player 2 doesn't have enough space
			CancelTradeResponse cancelTradePlayer1 = new CancelTradeResponse();
			cancelTradePlayer1.setRecoAndResponseText(0, String.format("%s doesn't have enough space.", otherPlayer.getDto().getName()));
			responseMaps.addClientOnlyResponse(player, cancelTradePlayer1);
			
			CancelTradeResponse cancelTradePlayer2 = new CancelTradeResponse();
			cancelTradePlayer1.setRecoAndResponseText(0, "you don't have enough space.");
			responseMaps.addClientOnlyResponse(otherPlayer, cancelTradePlayer2);
			return;
		}
		
		for (InventoryItemDto item : p1tradeItems.values())
			PlayerStorageDao.addItemToFirstFreeSlot(otherPlayer.getId(), StorageTypes.INVENTORY.getValue(), item.getItemId(), item.getCount(), item.getCharges());
		PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(player.getId(), StorageTypes.TRADE.getValue());
		
		for (InventoryItemDto item : p2tradeItems.values())
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), item.getItemId(), item.getCount(), item.getCharges());
		PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(otherPlayer.getId(), StorageTypes.TRADE.getValue());
		
		TradeManager.cancelTrade(trade);
		
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		new InventoryUpdateResponse().process(RequestFactory.create("", otherPlayer.getId()), otherPlayer, responseMaps);
		
		CancelTradeResponse cancelTrade = new CancelTradeResponse();
		responseMaps.addClientOnlyResponse(player, cancelTrade);
		responseMaps.addClientOnlyResponse(otherPlayer, cancelTrade);
	}
	
}
