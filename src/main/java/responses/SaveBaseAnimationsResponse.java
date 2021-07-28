package responses;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import database.dao.AnimationDao;
import database.dao.BaseAnimationsDao;
import database.dao.PlayerBaseAnimationsDao;
import database.dto.AnimationDto;
import database.dto.BaseAnimationsDto;
import database.dto.PlayerAnimationDto;
import database.dto.SaveBaseAnimationsDto;
import processing.attackable.Player;
import processing.managers.ClientResourceManager;
import processing.managers.LocationManager;
import requests.Request;
import requests.SaveBaseAnimationsRequest;
import types.PlayerPartType;

public class SaveBaseAnimationsResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof SaveBaseAnimationsRequest))
			return;
		
		boolean animationUpdate = false;
		SaveBaseAnimationsRequest request = (SaveBaseAnimationsRequest)req;
		for (SaveBaseAnimationsDto animation : request.getAnimations()) {
			PlayerPartType type = null;
			try {
				type = PlayerPartType.valueOf(animation.getPart().toUpperCase());
			} catch (IllegalArgumentException | NullPointerException e) {
				// if the user fucks with the request then this can throw an exception, so just bail silently
				continue;
			}
			if (!BaseAnimationsDao.getCustomizableParts().contains(type))
				continue; // it's a valid part, but not a customizable one
			
			AnimationDto currentAnimationDto = AnimationDao.getAnimationDtoByUpId(animation.getUpId());
			if (currentAnimationDto == null)
				continue; // the upId we were provided doesn't match a corresponding animation
			
			// baseAnimationId can be 0 - this means delete the entry from the db table
			final BaseAnimationsDto baseAnimation = BaseAnimationsDao.getBaseAnimationByAnimationId(currentAnimationDto.getId());
			if (baseAnimation == null)
				continue;
			animationUpdate |= PlayerBaseAnimationsDao.setAnimation(player.getId(), type, baseAnimation.getId(), animation.getColor());
		}
		
		if (animationUpdate) {
			final Map<PlayerPartType, PlayerAnimationDto> newAnimations = PlayerBaseAnimationsDao.getBaseAnimationsBasedOnEquipmentTypes(player.getId());
			
			PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
			playerUpdateResponse.setId(player.getId());
			playerUpdateResponse.setBaseAnimations(newAnimations);
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), playerUpdateResponse);
			
			LocationManager.getLocalPlayers(player.getFloor(), player.getTileId(), 12).forEach(localPlayer -> 
				ClientResourceManager.addAnimations(localPlayer, Collections.singleton(player.getId())));
		}
	}

}
