package main.responses;

import java.util.Set;

import lombok.Setter;
import main.database.SpriteMapDto;
import main.processing.Player;
import main.requests.Request;

public class AddResourceResponse extends Response {
	@Setter private Set<SpriteMapDto> spriteMaps = null;
	@Setter private Set<Integer> groundTextureSpriteMaps = null; // the spriteMapIds that exist in spriteMaps above
	
	public AddResourceResponse() {
		setAction("add_resources");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
}
