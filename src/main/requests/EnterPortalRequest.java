package main.requests;

import lombok.Getter;

public class EnterPortalRequest extends Request {
	public EnterPortalRequest() {
		setAction("enter");
	}
}
