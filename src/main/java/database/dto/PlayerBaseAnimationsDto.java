package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import types.PlayerPartType;

@Getter
@RequiredArgsConstructor
public class PlayerBaseAnimationsDto {
	private final int playerId;
	private final PlayerPartType playerPartType;
	private final int baseAnimationId;
	private final int color;
}
