package main.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItemDropFromNpcUpdate implements TybaltsTaskUpdate {
	private int itemId;
	private int count;
}
