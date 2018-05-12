package main.requests;

import main.PlayerRequestManager.PlayerRequestType;

public class DuelRequest extends PlayerRequest {

	@Override
	public PlayerRequestType getRequestType() {
		return PlayerRequestType.duel;
	}

}
