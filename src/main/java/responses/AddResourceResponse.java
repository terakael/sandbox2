package responses;

import java.util.Set;

import lombok.Setter;
import database.dto.GroundTextureDto;
import database.dto.ItemDto;
import database.dto.NPCDto;
import database.dto.SceneryDto;
import database.dto.SpriteFrameDto;
import database.dto.SpriteMapDto;
import processing.attackable.Player;
import requests.Request;

public class AddResourceResponse extends Response {
	@Setter private Set<SpriteMapDto> spriteMaps = null;
	@Setter private Set<Integer> groundTextureSpriteMaps = null; // the spriteMapIds that exist in spriteMaps above
	@Setter private Set<SpriteFrameDto> spriteFrames = null;
	@Setter private Set<ItemDto> items = null;
	@Setter private Set<SceneryDto> scenery = null;
	@Setter private Set<NPCDto> npcs = null;
	@Setter private Set<GroundTextureDto> groundTextures = null;
	
	public AddResourceResponse() {
		setAction("add_resources");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
}
