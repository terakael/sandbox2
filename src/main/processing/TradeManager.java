package main.processing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import main.database.InventoryItemDto;
import main.database.PlayerStorageDao;
import main.types.StorageTypes;

public class TradeManager {
	private TradeManager() {}
	
	private static ArrayList<Trade> trades = new ArrayList<>();
	public static class Trade {
		private Player p1;
		private Player p2;
		boolean p1accepted = false;
		boolean p2accepted = false;
		public Trade(Player p1, Player p2) {
			this.p1 = p1;
			this.p2 = p2;
		}
		
		public Map<Integer, InventoryItemDto> getItemsByPlayer(Player p) {
			if (hasPlayer(p))
				return PlayerStorageDao.getStorageDtoMapByPlayerId(p.getId(), StorageTypes.TRADE);
			return null;
		}
		
		public boolean hasPlayer(Player p) {
			return p == p1 || p == p2;
		}
		
		public Player getOtherPlayer(Player player) {
			if (player == p1)
				return p2;
			return p1;
		}
		
		public void playerAcceptsTrade(Player player) {
			if (player.equals(p1))
				p1accepted = true;
			else if (player.equals(p2))
				p2accepted = true;
		}
		
		public boolean bothPlayersAccepted() {
			return p1accepted && p2accepted;
		}
		
		public HashSet<Integer> getAcceptedPlayerIds() {
			HashSet<Integer> acceptedPlayerIds = new HashSet<>();
			if (p1accepted)
				acceptedPlayerIds.add(p1.getId());
			
			if (p2accepted)
				acceptedPlayerIds.add(p2.getId());
				
			return acceptedPlayerIds;
		}
		
		public void cancelAccepts() {
			p1accepted = false;
			p2accepted = false;
		}
	}
	
	public static Trade getTradeWithPlayer(Player p) {
		for (Trade trade : trades) {
			if (trade.hasPlayer(p))
				return trade;
		}
		return null;
	}
	
	public static void addTrade(Player p1, Player p2) {
		trades.add(new Trade(p1, p2));
	}
	
	public static void cancelTrade(Player p) {
		Trade trade = getTradeWithPlayer(p);
		if (trade != null)
			cancelTrade(trade);
	}
	
	public static void cancelTrade(Trade trade) {
		trades.remove(trade);
	}
	
	public static void playerAcceptsTrade(Player p) {
		Trade trade = getTradeWithPlayer(p);
		if (trade == null)
			return;
		
		trade.playerAcceptsTrade(p);
	}
}
