package main.database;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class SceneryDto {
	private int id;
	private String name;
	private int spriteMapId;
	private int x;
	private int y;
	private int w;
	private int h;
	private float anchorX;
	private float anchorY;
	private int framecount;
	private int framerate;
	private int attributes;
	private ArrayList<Integer> instances = null;
}
