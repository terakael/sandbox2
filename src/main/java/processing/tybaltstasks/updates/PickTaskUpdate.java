package processing.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PickTaskUpdate implements TybaltsTaskUpdate {
	private int pickedItemId;
}
