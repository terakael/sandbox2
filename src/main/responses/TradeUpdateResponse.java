package main.responses;

import java.util.HashMap;

import main.database.InventoryItemDto;
import main.database.PlayerStorageDao;
import main.processing.Player;
import main.processing.TradeManager;
import main.processing.TradeManager.Trade;
import main.requests.Request;
import main.types.StorageTypes;

public class TradeUpdateResponse extends Response {
	private HashMap<Integer, InventoryItemDto> playerTradeData = new HashMap<>();
	private HashMap<Integer, InventoryItemDto> otherTradeData = new HashMap<>();
	
	public TradeUpdateResponse() {
		setAction("trade_update");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		Trade trade = TradeManager.getTradeWithPlayer(player);
		if (trade == null)
			return;
		
		Player otherPlayer = trade.getOtherPlayer(player);
		if (otherPlayer == null)
			return;
		
		playerTradeData = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.TRADE.getValue());
		otherTradeData = PlayerStorageDao.getStorageDtoMapByPlayerId(otherPlayer.getId(), StorageTypes.TRADE.getValue());
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
