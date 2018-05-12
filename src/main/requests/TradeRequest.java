package main.requests;

import main.PlayerRequestManager.PlayerRequestType;

public class TradeRequest extends PlayerRequest {

	@Override
	public PlayerRequestType getRequestType() {
		return PlayerRequestType.trade;
	}

}
