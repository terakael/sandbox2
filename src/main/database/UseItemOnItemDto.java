package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UseItemOnItemDto {
	private int srcId;
	private int destId;
	private int requiredSrcCount;
	private int resultingItemId;
	private int resultingItemCount;
	private boolean keepSrcItem;
}
