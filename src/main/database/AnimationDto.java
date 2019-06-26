package main.database;

import lombok.AllArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
public class AnimationDto {
	private int up;
	private int down;
	private int left;
	private int right;
	private int attack_left;
	private int attack_right;
}
