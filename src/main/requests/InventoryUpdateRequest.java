package main.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InventoryUpdateRequest extends Request {
	private int slot;
}
