package main.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class TargetRequest extends Request {
	private int targetX;
	private int targetY;
}
