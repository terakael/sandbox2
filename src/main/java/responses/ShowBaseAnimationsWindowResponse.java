package responses;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import database.dao.BaseAnimationsDao;
import database.dao.PlayerBaseAnimationsDao;
import database.dto.PlayerAnimationDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import requests.Request;
import types.PlayerPartType;

public class ShowBaseAnimationsWindowResponse extends Response {
	private Map<PlayerPartType, PlayerAnimationDto> baseAnimations = null;
	private Map<PlayerPartType, PlayerAnimationDto> customizableAnimations = null;
	
	public ShowBaseAnimationsWindowResponse() {
		setAction("show_base_animations_window");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		final Map<PlayerPartType, PlayerAnimationDto> playerAnimations = PlayerBaseAnimationsDao.loadAnimationsByPlayerId(player.getId());
		baseAnimations = new LinkedHashMap<>();
		baseAnimations.put(PlayerPartType.HEAD, playerAnimations.get(PlayerPartType.HEAD));
		baseAnimations.put(PlayerPartType.TORSO, playerAnimations.get(PlayerPartType.TORSO));
		baseAnimations.put(PlayerPartType.LEGS, playerAnimations.get(PlayerPartType.LEGS));
		
		final PlayerAnimationDto dummy = new PlayerAnimationDto(0, 0, 0, 0, 0, 0, null);
		customizableAnimations = new LinkedHashMap<>();
		BaseAnimationsDao.getCustomizableParts().forEach(part -> {
			customizableAnimations.put(part, playerAnimations.containsKey(part) ? playerAnimations.get(part) : dummy);
		});
		
		responseMaps.addClientOnlyResponse(player, this);
		ClientResourceManager.addAnimationDtos(player, new HashSet<>(playerAnimations.values()));
	}

}
