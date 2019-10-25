package main.requests;

import lombok.Getter;

@Getter
public class UseRequest extends Request {
	int src;
	int dest;
	String type;// what is the dest id's object type (scenery, item, player, npc)
	int srcSlot;
	int slot;
}
