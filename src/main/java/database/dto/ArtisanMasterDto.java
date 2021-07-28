package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ArtisanMasterDto {
	private final int npcId;
	private final int artisanRequirement;
	private final int assignmentLevelRangeMin;
	private final int assignmentLevelRangeMax;
	private final int completionPoints;
}
