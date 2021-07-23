package requests;

import system.PlayerRequestManager.PlayerRequestType;

public class DuelRequest extends PlayerRequest {

	@Override
	public PlayerRequestType getRequestType() {
		return PlayerRequestType.duel;
	}

}
