package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConsumableDto {
	private int itemId;
	private int becomesId;// itemId it turns into on consumption (e.g. goblin stank (4) -> goblin stank (3))
}
