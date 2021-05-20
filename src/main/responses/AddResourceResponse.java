package main.responses;

import java.util.Set;

import lombok.Setter;
import main.database.GroundTextureDto;
import main.database.ItemDto;
import main.database.NPCDto;
import main.database.SceneryDto;
import main.database.SpriteFrameDto;
import main.database.SpriteMapDto;
import main.processing.Player;
import main.requests.Request;

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
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
}
