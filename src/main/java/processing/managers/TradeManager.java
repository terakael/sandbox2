package processing.managers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import types.StorageTypes;

// despite being called TradeManager, this is used for both trades and duels
public class TradeManager {
	private TradeManager() {}
	
	private static ArrayList<Trade> trades = new ArrayList<>();
	public static class Trade {
		private Player p1;
		private Player p2;
		boolean p1accepted = false;
		boolean p2accepted = false;
		@Getter @Setter private Integer p1Rules = null; // null for a trade, non-null for a duel
		@Getter @Setter private Integer p2Rules = null; // null for a trade, non-null for a duel
		
		public Trade(Player p1, Player p2, boolean isDuel) {
			this.p1 = p1;
			this.p2 = p2;
			p1Rules = isDuel ? 0 : null;
			p2Rules = isDuel ? 0 : null;
		}
		
		public Map<Integer, InventoryItemDto> getItemsByPlayer(Player p) {
			if (hasPlayer(p))
				return PlayerStorageDao.getStorageDtoMapByPlayerId(p.getId(), StorageTypes.TRADE);
			return null;
		}
		
		public boolean hasPlayer(Player p) {
			return p == p1 || p == p2;
		}
		
		public boolean playerIsP1(Player p) {
			return p == p1;
		}
		
		public Player getOtherPlayer(Player player) {
			return (player == p1) ? p2 : p1;
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
		
		public boolean isDuel() {
			return p1Rules != null;
		}
	}
	
	public static Trade getTradeWithPlayer(Player p) {
		for (Trade trade : trades) {
			if (trade.hasPlayer(p))
				return trade;
		}
		return null;
	}
	
	public static void addTrade(Player p1, Player p2, boolean isDuel) {
		trades.add(new Trade(p1, p2, isDuel));
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
