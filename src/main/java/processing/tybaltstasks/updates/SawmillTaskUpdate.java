package processing.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SawmillTaskUpdate implements TybaltsTaskUpdate {
	private int createdPlankId;
}
