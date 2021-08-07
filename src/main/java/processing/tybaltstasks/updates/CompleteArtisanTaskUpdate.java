package processing.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class CompleteArtisanTaskUpdate implements TybaltsTaskUpdate {
	@Getter private int assignedMasterId;
}
