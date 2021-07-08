package main.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ConstructTaskUpdate implements TybaltsTaskUpdate {
	@Getter private int sceneryId;
}
