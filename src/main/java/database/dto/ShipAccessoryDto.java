package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ShipAccessoryDto {
	private final int id;
	private final String name;
	private final int spriteFrameId;
	private final int level;
	private final int primaryMaterialId;
	private final int primaryMaterialCount;
	private final int secondaryMaterialId;
	private final int secondaryMaterialCount;
	private final int offense;
	private final int defense;
	private final int fishing;
	private final int storage;
	private final int crew;
}
