package requests;

import lombok.Getter;

public class ToggleDuelRuleRequest extends MultiRequest {
	@Getter private int rule;
}
