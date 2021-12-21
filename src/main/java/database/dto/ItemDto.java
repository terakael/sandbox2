package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter 
@RequiredArgsConstructor
public class ItemDto {
	private final int id;
	private final String name;
	private final String namePlural;
	private final int spriteFrameId;
	private final int leftclickOption;
	private final int otherOptions;
	private final int attributes;
	private final int price;
	private final int shiftclickOption;
}
