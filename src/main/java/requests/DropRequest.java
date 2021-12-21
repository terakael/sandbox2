package requests;

import lombok.Getter;

public class DropRequest extends MultiRequest {
	@Getter private int slot;
}
