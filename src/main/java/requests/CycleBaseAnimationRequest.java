package requests;

import lombok.Getter;

@Getter
public class CycleBaseAnimationRequest extends MultiRequest {
	private String direction; // previous, next
	private String part; // hair, beard, shirt, shoes etc
	private int currentlyDisplayedUpId; // so we know the next one
	private int color; // keep the colour the same as what was already set
}
