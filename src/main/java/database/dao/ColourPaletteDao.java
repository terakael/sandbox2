package database.dao;

import java.util.LinkedList;
import java.util.List;

import database.DbConnection;
import lombok.Getter;

public class ColourPaletteDao {
	@Getter private static List<Integer> palette = new LinkedList<>();
	
	public static void setupCaches() {
		DbConnection.load(
				"select colour from colour_palette where palette_id=1 order by ordinal", 
				rs -> palette.add(rs.getInt("colour"))
		);
	}
}
