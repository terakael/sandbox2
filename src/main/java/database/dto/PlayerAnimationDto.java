package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class PlayerAnimationDto {
	public PlayerAnimationDto(AnimationDto dto, Integer color) {
		this.up = dto.getUpId();
		this.down = dto.getDownId();
		this.left = dto.getLeftId();
		this.right = dto.getRightId();
		this.attack_left = dto.getAttackLeftId();
		this.attack_right = dto.getAttackRightId();
		this.color = color;
	}
	
	private int up;
	private int down;
	private int left;
	private int right;
	private int attack_left;
	private int attack_right;
	private Integer color;
}
