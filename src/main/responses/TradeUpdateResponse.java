package main.responses;

import java.util.HashMap;
import java.util.Map;

import main.database.dao.PlayerStorageDao;
import main.database.dto.InventoryItemDto;
import main.processing.Player;
import main.processing.TradeManager;
import main.processing.TradeManager.Trade;
import main.requests.Request;
import main.types.StorageTypes;

@SuppressWarnings("unused")
public class TradeUpdateResponse extends Response {
	private Map<Integer, InventoryItemDto> playerTradeData = new HashMap<>();
	private Map<Integer, InventoryItemDto> otherTradeData = new HashMap<>();
	
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
		
		playerTradeData = PlayerStorageDao.getStorageDtoMapByPlayerId(player.getId(), StorageTypes.TRADE);
		otherTradeData = PlayerStorageDao.getStorageDtoMapByPlayerId(otherPlayer.getId(), StorageTypes.TRADE);
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
