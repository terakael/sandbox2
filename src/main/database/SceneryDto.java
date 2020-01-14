package main.database;

import java.util.ArrayList;
import java.util.HashSet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class SceneryDto {
	@Getter private int id;
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
	private int leftclickOption;
	private int otherOptions;
	@Getter private HashSet<Integer> instances = null;
	private int attributes;
}
