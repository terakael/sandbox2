package processing.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SmithTaskUpdate implements TybaltsTaskUpdate {
	private int smithedItemId;
}
