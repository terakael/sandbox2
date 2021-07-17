package main.processing.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CookTaskUpdate implements TybaltsTaskUpdate {
	private int cookedItemId;
	private boolean burnt;
}
