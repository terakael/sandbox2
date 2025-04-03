package processing.attackable.bosses;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import database.dao.NPCDao;
import processing.PathFinder;
import processing.attackable.NPC;
import processing.managers.ClientResourceManager;
import processing.managers.LocationManager;
import responses.CastSpellResponse;
import responses.ResponseMaps;
import utils.Utils;

public class Kraken extends NPC {
	private List<NPC> tentacles = null;
	private int currentSpellTimer = 0;
	private static final Set<Pair<Integer, Integer>> offsets = Set.<Pair<Integer, Integer>>of(
			// three behind
			Pair.of(-2, -2), Pair.of(0, -2), Pair.of(2, -2),

			// three in front
			Pair.of(-2, 2), Pair.of(0, 2), Pair.of(2, 2),

			// one each side
			Pair.of(-3, 0), Pair.of(3, 0));

	public Kraken(int floor, int tileId) {
		super(NPCDao.getNpcById(63), floor, tileId);

		LocationManager.addNpcs(Collections.singletonList(this));

		tentacles = offsets.stream()
				.map(offset -> new KrakenTentacle(floor,
						tileId + offset.getLeft() + (offset.getRight() * PathFinder.LENGTH)))
				.collect(Collectors.toList());
		LocationManager.addNpcs(tentacles);
	}

	@Override
	protected boolean popPath(ResponseMaps responseMaps) {
		// he doesn't move
		return false;
	}

	@Override
	protected void handleActiveTarget(ResponseMaps responseMaps) {
		if (++currentSpellTimer % 10 != 0)
			return;

		if (!Utils.areTileIdsWithinRadius(target.getTileId(), tileId, 8))
			return;

		// kraken casts spells n shit
		final CastSpellResponse projectile = new CastSpellResponse(tileId, target.getTileId(), "tile", 567, 4, 0.25);
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
