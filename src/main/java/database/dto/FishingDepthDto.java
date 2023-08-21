package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FishingDepthDto {
	private final int itemId;
	private final int idealDepth;
	private final int variance;
}
