package processing.attackable.bosses;

import java.util.Collections;

import database.dao.NPCDao;
import processing.PathFinder;
import processing.attackable.NPC;
import processing.managers.ClientResourceManager;
import processing.managers.LocationManager;
import responses.CastSpellResponse;
import responses.ResponseMaps;
import utils.RandomUtil;
import utils.Utils;

public class KrakenTentacle extends NPC {
	private int currentSpellTimer = 0;
	private static final int spellTimer = 5;

	public KrakenTentacle(int floor, int instanceId) {
		super(NPCDao.getNpcById(64), floor, instanceId);
		currentSpellTimer = RandomUtil.getRandom(0, spellTimer);
	}

	@Override
	protected boolean popPath(ResponseMaps responseMaps) {
		// he doesn't move
		return false;
	}

	@Override
	protected void handleActiveTarget(ResponseMaps responseMaps) {
		if (++currentSpellTimer % spellTimer != 0)
			return;

		if (!Utils.areTileIdsWithinRadius(target.getTileId(), tileId, 8))
			return;

		// kraken casts spells n shit
		final CastSpellResponse projectile = new CastSpellResponse(tileId, target.getTileId(), "tile", 567, 1, 1);
		responseMaps.addLocalResponse(floor, tileId, projectile);

		LocationManager.getLocalPlayers(floor, tileId, 12)
				.forEach(localPlayer -> ClientResourceManager.addSpriteFramesAndSpriteMaps(localPlayer,
						Collections.singleton(567)));
	}

	@Override
	protected void setPathToRandomTileInRadius(ResponseMaps responseMaps) {
		// kraken doesn't move so no path
	}

	@Override
	public void onRespawn(ResponseMaps responseMaps) {
		LocationManager.removeNpc(this);
	}

	@Override
	protected void findTarget(int currentTick, ResponseMaps responseMaps) {
		target = LocationManager.getLocalPlayers(floor, tileId, 8).stream()
				.sorted((p1, p2) -> PathFinder.getCloserTile(tileId, p1.getTileId(), p2.getTileId()))
				.findFirst()
				.orElse(null);
	}
}
