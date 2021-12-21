package requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InventoryUpdateRequest extends MultiRequest {
	private int slot;
}
