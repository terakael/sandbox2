package main.requests;

import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class InventoryMoveRequest extends MultiRequest {
	private int src;
	private int dest;
}
