package main.requests;

import lombok.Getter;

public class EquipRequest extends MultiRequest {
	@Getter private int slot;
}
