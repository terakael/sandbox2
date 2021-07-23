package responses;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.WorldProcessor;
import processing.attackable.Player;
import processing.managers.FightManager;
import processing.managers.TradeManager;
import processing.managers.TradeManager.Trade;
import requests.Request;
import requests.RequestFactory;
import types.DuelRules;
import types.StorageTypes;
import utils.RandomUtil;

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
		
		if (trade.isDuel() && trade.getP1Rules() != trade.getP2Rules()) {
			setRecoAndResponseText(1, "the rules must match before you can accept.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
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
		
		if (trade.isDuel()) {
			if (trade.getP1Rules() != trade.getP2Rules())
				return;// both players have to have agreed-upon rules
			
			Player player1 = WorldProcessor.getPlayerById(player.getId());
			Player player2 = WorldProcessor.getPlayerById(otherPlayer.getId());
			FightManager.addFightWithRules(player1, player2, RandomUtil.getRandom(0, 100) < 50, trade.getP1Rules());
			
			// run through and set up the rules
			// turn off any prayer if that's the rules
			if ((trade.getP1Rules() & DuelRules.no_prayer.getValue()) > 0) {
				player1.clearActivePrayers(responseMaps);				
				player2.clearActivePrayers(responseMaps);
			}
			
			if ((trade.getP1Rules() & DuelRules.no_boosted_stats.getValue()) > 0) {
				// set all combat stats to normal
				player1.removeCombatBoosts(responseMaps);
				player2.removeCombatBoosts(responseMaps);
			}
			
			player1.setTileId(player2.getTileId());
			
			PvpStartResponse pvpStart = new PvpStartResponse();
			pvpStart.setPlayer1Id(player1.getId());
			pvpStart.setPlayer2Id(player2.getId());
			pvpStart.setTileId(player2.getTileId());
			responseMaps.addBroadcastResponse(pvpStart);
			
			TradeManager.cancelTrade(trade);
			
			CancelTradeResponse cancelTrade = new CancelTradeResponse();
			responseMaps.addClientOnlyResponse(player, cancelTrade);
			responseMaps.addClientOnlyResponse(otherPlayer, cancelTrade);
			return;
		}
		
		List<Integer> p1invItems = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		List<Integer> p2invItems = PlayerStorageDao.getStorageListByPlayerId(otherPlayer.getId(), StorageTypes.INVENTORY);
		
		Map<Integer, InventoryItemDto> p1tradeItems = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.TRADE);
		Map<Integer, InventoryItemDto> p2tradeItems = PlayerStorageDao.getStorageDtoMapByPlayerId(otherPlayer.getId(), StorageTypes.TRADE);
		
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
			PlayerStorageDao.addItemToFirstFreeSlot(otherPlayer.getId(), StorageTypes.INVENTORY, item.getItemId(), item.getCount(), item.getCharges());
		PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(player.getId(), StorageTypes.TRADE);
		
		for (InventoryItemDto item : p2tradeItems.values())
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, item.getItemId(), item.getCount(), item.getCharges());
		PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(otherPlayer.getId(), StorageTypes.TRADE);
		
		TradeManager.cancelTrade(trade);
		
		new InventoryUpdateResponse().process(RequestFactory.create("", player.getId()), player, responseMaps);
		new InventoryUpdateResponse().process(RequestFactory.create("", otherPlayer.getId()), otherPlayer, responseMaps);
		
		CancelTradeResponse cancelTrade = new CancelTradeResponse();
		responseMaps.addClientOnlyResponse(player, cancelTrade);
		responseMaps.addClientOnlyResponse(otherPlayer, cancelTrade);
	}
	
}
