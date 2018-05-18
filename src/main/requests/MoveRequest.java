package main.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MoveRequest extends Request {
	//private int destinationTileId; // TODO
	private int x;
	private int y;
}
