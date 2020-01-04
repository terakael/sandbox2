package main.processing;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.imageio.ImageIO;

import main.database.GroundTextureDao;
import main.database.GroundTextureDto;
import main.database.MineableDao;
import main.database.SceneryDao;
import main.database.SpriteMapDao;

public class MinimapGenerator {
	private static BufferedImage image;
	public static void createImage() throws IOException {
		image = new BufferedImage(250, 250, BufferedImage.TYPE_INT_RGB);
		
		drawStandardGroundTextureDataToImage(image);

		HashSet<Integer> woodenFloorInstances = GroundTextureDao.getInstancesByGroundTextureId(1).get(1);
		for (int tileId : woodenFloorInstances) {
			image.setRGB(tileId % 250, tileId / 250, 4665613);
		}
		
		// after the ground textures, we can add the rest of the scenery
		
		for (int i = 31; i <= 46; ++i) {
			HashSet<Integer> instances = SceneryDao.getInstanceListByRoomIdAndSceneryId(1, i);
			for (int tileId : instances) {
				image.setRGB(tileId % 250, tileId / 250, Color.BLACK.getRGB());
			}
		}
		
		for (Map.Entry<Integer, HashSet<Integer>> entry : MineableDao.getMineableInstances().entrySet()) {
			for (int tileId : entry.getValue()) {
				image.setRGB(tileId % 250, tileId / 250, Color.GRAY.getRGB());
			}
		}
		
		// trees
		for (int i = 1; i <= 9; ++i) {
			HashSet<Integer> instances = SceneryDao.getInstanceListByRoomIdAndSceneryId(1, i);
			for (int tileId : instances) {
				image.setRGB(tileId % 250, tileId / 250, 1324044);
			}
		}
		
		// flowers
		for (int i = 10; i <= 15; ++i) {
			HashSet<Integer> instances = SceneryDao.getInstanceListByRoomIdAndSceneryId(1, i);
			for (int tileId : instances) {
				image.setRGB(tileId % 250, tileId / 250, 8625177);
			}
		}
		
		// fire
		int[] fireIds = {20,  47, 48};
		for (int fireId : fireIds ) {
			HashSet<Integer> fireInstances = SceneryDao.getInstanceListByRoomIdAndSceneryId(1, fireId);
			for (int tileId : fireInstances) {
				image.setRGB(tileId % 250, tileId / 250, Color.RED.getRGB());
			}
		}
		
		// furnace
		HashSet<Integer> furnaceInstances = SceneryDao.getInstanceListByRoomIdAndSceneryId(1, 19);
		for (int tileId : furnaceInstances) {
			image.setRGB(tileId % 250, tileId / 250, Color.ORANGE.getRGB());
		}
		
		// obelisks
		for (int i = 21; i <= 28; ++i) {
			HashSet<Integer> instances = SceneryDao.getInstanceListByRoomIdAndSceneryId(1, i);
			for (int tileId : instances) {
				image.setRGB(tileId % 250, tileId / 250, Color.WHITE.getRGB());
			}
		}
		
		// ladders
		HashSet<Integer> instances = SceneryDao.getInstanceListByRoomIdAndSceneryId(1, 50);
		for (int tileId : instances) {
			image.setRGB(tileId % 250, tileId / 250, Color.BLACK.getRGB());
		}
		
		File outputfile = new File("D:\\github\\brackets\\img\\map.png");
		ImageIO.write(image, "png", outputfile);
	}
	
	private static void drawStandardGroundTextureDataToImage(BufferedImage image) throws IOException {
		HashMap<Integer, Integer> primaryAveragesBySpriteMapId = new HashMap<>();
		HashMap<Integer, Integer> opposingAveragesBySpriteMapId = new HashMap<>();
		
		HashSet<Integer> standardGroundTextureSpriteMaps = GroundTextureDao.getStandardTileSetSpriteMaps();
		for (int spriteMapId : standardGroundTextureSpriteMaps) {
			InputStream is = SpriteMapDao.getSpriteMapImageBinaryStreamById(spriteMapId);
			if (is == null)
				continue;
			
			BufferedImage spriteMap = ImageIO.read(is);
			
			// primary tile is X07 (107, 207 etc), which is x=32, y=32 (w and h are always 32/32)
			int pR = 0;
			int pG = 0;
			int pB = 0;
			for (int y = 32; y < 64; ++y) {
				for (int x = 32; x < 64; ++x) {
					Color color = new Color(spriteMap.getRGB(x, y));
					pR += color.getRed();
					pG += color.getGreen();
					pB += color.getBlue();
				}
			}
			Color pAverage = new Color(pR / (32*32), pG / (32*32), pB / (32*32));
			primaryAveragesBySpriteMapId.put(spriteMapId, pAverage.getRGB());
			
			// opposing tile is X28 (128, 228 etc), which is x=64, y=160
			int oR = 0;
			int oG = 0;
			int oB = 0;
			for (int y = 160; y < 192; ++y) {
				for (int x = 64; x < 96; ++x) {
					Color color = new Color(spriteMap.getRGB(x, y));
					oR += color.getRed();
					oG += color.getGreen();
					oB += color.getBlue();
				}
			}
			Color oAverage = new Color(oR / (32*32), oG / (32*32), oB / (32*32));
			opposingAveragesBySpriteMapId.put(spriteMapId, oAverage.getRGB());
		}
		
		for (GroundTextureDto dto : GroundTextureDao.getAllGroundTexturesByRoom(1)) {
			if (!standardGroundTextureSpriteMaps.contains(dto.getSpriteMapId()))			
				continue;
			
			for (int tileId : dto.getInstances()) {
				image.setRGB(tileId % 250, tileId / 250, (dto.getId() - 28) % 100 == 0 ? opposingAveragesBySpriteMapId.get(dto.getSpriteMapId()) : primaryAveragesBySpriteMapId.get(dto.getSpriteMapId()));
			}
		}
	}
	
	public static String getImage() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		try {
			ImageIO.write(image, "png", os);
			return Base64.getEncoder().encodeToString(os.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "";
	}
}
