package main.responses;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import main.Stats;
import main.database.DbConnection;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerSessionDao;
import main.database.StatsDao;
import main.requests.LogonRequest;
import main.requests.Request;

public class LogonResponse extends Response {
	
	@Getter private String userId;
	@Getter private String username;
	@Getter private int x;
	@Getter private int y;
	private Stats stats;
	private List<PlayerDto> players;

	public LogonResponse(String action) {
		super(action);
	}
	
	@Override
	public boolean process(Request req) {
		if (!(req instanceof LogonRequest)) {
			setRecoAndResponseText(0, "funny business");
			return false;
		}
		
		LogonRequest logonReq = (LogonRequest)req;
		PlayerDao dao = new PlayerDao(DbConnection.get());
		
		PlayerDto dto = dao.getPlayerByUsernameAndPassword(logonReq.getUsername(), logonReq.getPassword());
		if (dto == null) {
			setRecoAndResponseText(0, "invalid credentials");
			return false;
		} else {
			if (PlayerSessionDao.entryExists(dto.getId())) {
				setRecoAndResponseText(0, "already logged in");
				return false;
			}
			dao.updateLastLoggedIn(dto.getId());
		}
		
		userId = Integer.toString(dto.getId());
		username = dto.getName();
		
		x = dto.getX();
		y = dto.getY();
		
		StatsDao statsDao = new StatsDao(DbConnection.get());
		Map<String, Integer> statList = statsDao.getStatsByPlayerId(dto.getId());
		stats = new Stats(statList);
		
		players = PlayerDao.getAllPlayers();

		setRecoAndResponseText(1, "");
		return false;
	}
}
