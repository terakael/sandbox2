package main.processing.tybaltstasks.updates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ChopTaskUpdate implements TybaltsTaskUpdate {
	@Getter private int treeId;
}
