package processing.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FishTaskUpdate implements TybaltsTaskUpdate {
	private int fishedItemId;
}
