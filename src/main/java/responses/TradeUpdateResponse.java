package responses;

import java.util.HashMap;
import java.util.Map;

import database.dao.PlayerStorageDao;
import database.dto.InventoryItemDto;
import processing.attackable.Player;
import processing.managers.TradeManager;
import processing.managers.TradeManager.Trade;
import requests.Request;
import types.StorageTypes;

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
