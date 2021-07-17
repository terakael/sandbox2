package main.processing.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TakeTaskUpdate implements TybaltsTaskUpdate {
	private int pickedUpItemId;
	private int count;
}
