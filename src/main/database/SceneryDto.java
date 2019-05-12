package main.database;

import lombok.AllArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
public class SceneryDto {
	private String name;
	private int sprite_map_id;
	private int x;
	private int y;
	private int w;
	private int h;
	private float anchor_x;
	private float anchor_y;
	private int framecount;
	private int framerate;
	private int attributes;
}
