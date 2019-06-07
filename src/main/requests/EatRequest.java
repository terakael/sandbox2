package main.requests;

import lombok.Getter;

public class EatRequest extends Request {
	@Getter private int objectId;
	@Getter private int slot;
}
