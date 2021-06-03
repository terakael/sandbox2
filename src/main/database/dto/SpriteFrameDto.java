package main.database.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class SpriteFrameDto {
	private int id;
	private int sprite_map_id;
	private int x;
	private int y;
	private int w;
	private int h;
	private float anchorX;
	private float anchorY;
	private float scaleX;
	private float scaleY;
	private int margin;
	private int frame_count;
	private int framerate;
	private int animation_type_id;
	private Map<Integer, CustomBoundingBoxDto> customBoundingBoxes;
	private int color;
}
