package main.requests;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConstructionRequest extends Request {
	private int sceneryId;
	private boolean flatpack;
}
