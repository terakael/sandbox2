package main.requests;

import lombok.Getter;

public class EnterPortalRequest extends Request {
	@Getter private int tileId;
	
	public EnterPortalRequest() {
		setAction("enter");
	}
}
