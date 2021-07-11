package main.database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class NPCDto {
	public NPCDto(NPCDto that) {
		this.id = that.id;
		this.name = that.name;
		this.upId = that.upId;
		this.downId = that.downId;
		this.leftId = that.leftId;
		this.rightId = that.rightId;
		this.attackId = that.attackId;
		this.scaleX = that.scaleX;
		this.scaleY = that.scaleY;
		this.tileId = that.tileId;
		this.hp = that.hp;
		this.cmb = that.cmb;
		this.leftclickOption = that.leftclickOption;
		this.otherOptions = that.otherOptions;
		this.floor = that.floor;
		this.acc = that.acc;
		this.str = that.str;
		this.def = that.def;
		this.pray = that.pray;
		this.magic = that.magic;
		this.accBonus = that.accBonus;
		this.strBonus = that.strBonus;
		this.defBonus = that.defBonus;
		this.prayBonus = that.prayBonus;
		this.attackSpeed = that.attackSpeed;
		this.roamRadius = that.roamRadius;
		this.attributes = that.attributes;
		this.respawnTicks = that.respawnTicks;
	}
	
	private int id;
	private String name;
	private int upId;
	private int downId;
	private int leftId;
	private int rightId;
	private int attackId;
	private float scaleX;
	private float scaleY;
	@Setter private int tileId;
	private int hp;
	private int cmb;
	private int leftclickOption;
	private int otherOptions;
	@Setter private int floor;
	
	// below are data that doesn't get sent to client as the client doesn't need to see these
	private transient int acc;
	private transient int str;
	private transient int def;
	private transient int pray;
	private transient int magic;
	private transient int accBonus;
	private transient int strBonus;
	private transient int defBonus;
	private transient int prayBonus;
	private transient int attackSpeed;
	private transient int roamRadius;
	private transient int attributes;
	private transient int respawnTicks;
}
