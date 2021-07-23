package requests;

import system.PlayerRequestManager.PlayerRequestType;

public class TradeRequest extends PlayerRequest {

	@Override
	public PlayerRequestType getRequestType() {
		return PlayerRequestType.trade;
	}

}
