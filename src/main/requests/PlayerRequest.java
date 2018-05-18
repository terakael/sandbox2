package main.requests;

import lombok.Getter;
import lombok.Setter;
import main.PlayerRequestManager;

@Getter @Setter
public abstract class PlayerRequest extends Request {
	private int objectId; // other player id
	public abstract PlayerRequestManager.PlayerRequestType getRequestType();
}
