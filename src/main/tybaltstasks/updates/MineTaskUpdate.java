package main.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class MineTaskUpdate implements TybaltsTaskUpdate {
	@Getter private int minedItemId;
	@Getter private int count; // gold/silver can have multiple
}
