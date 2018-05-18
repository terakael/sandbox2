package main.requests;

import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class InventoryMoveRequest extends Request {
	private int src;
	private int dest;
}
