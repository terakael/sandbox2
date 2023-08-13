package types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConstructionLandTypes {
	land(1),
	water(2);
	
	private final int value;
}
