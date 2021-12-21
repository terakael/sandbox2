package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SmithableDto {
	private int itemId;
	private int level;
	private int barId;
	private int requiredBars;
}
