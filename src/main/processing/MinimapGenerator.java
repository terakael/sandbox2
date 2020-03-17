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
	private static HashMap<Integer, BufferedImage> images = new HashMap<>();
	public static void createImage(int roomId) throws IOException {
		BufferedImage image = new BufferedImage(250, 250, BufferedImage.TYPE_INT_RGB); 
		
		drawStandardGroundTextureDataToImage(roomId, image);

		HashSet<Integer> woodenFloorInstances = GroundTextureDao.getInstancesByGroundTextureId(1).get(roomId);
		if (woodenFloorInstances != null) {
			for (int tileId : woodenFloorInstances) {
				image.setRGB(tileId % 250, tileId / 250, 4665613);
			}
		}
		
		// after the ground textures, we can add the rest of the scenery
		for (int i = 31; i <= 46; ++i) {
			HashSet<Integer> instances = SceneryDao.getInstanceListByRoomIdAndSceneryId(roomId, i);
			for (int tileId : instances) {
				image.setRGB(tileId % 250, tileId / 250, Color.BLACK.getRGB());
			}
		}
		
		if (MineableDao.getMineableInstances().containsKey(roomId)) {
			for (Map.Entry<Integer, HashSet<Integer>> entry : MineableDao.getMineableInstances().get(roomId).entrySet()) {
				for (int tileId : entry.getValue()) {
					image.setRGB(tileId % 250, tileId / 250, Color.GRAY.getRGB());
				}
			}
		}
		
		// trees
		for (int i = 1; i <= 9; ++i) {
			HashSet<Integer> instances = SceneryDao.getInstanceListByRoomIdAndSceneryId(roomId, i);
			for (int tileId : instances) {
				image.setRGB(tileId % 250, tileId / 250, 1324044);
			}
		}
		
		// flowers
		for (int i = 10; i <= 15; ++i) {
			HashSet<Integer> instances = SceneryDao.getInstanceListByRoomIdAndSceneryId(roomId, i);
			for (int tileId : instances) {
				image.setRGB(tileId % 250, tileId / 250, 8625177);
			}
		}
		
		// fire
		int[] fireIds = {20,  47, 48};
		for (int fireId : fireIds ) {
			HashSet<Integer> fireInstances = SceneryDao.getInstanceListByRoomIdAndSceneryId(roomId, fireId);
			for (int tileId : fireInstances) {
				image.setRGB(tileId % 250, tileId / 250, Color.RED.getRGB());
			}
		}
		
		int[] ladderIds = {50, 60};
		for (int ladderId : ladderIds) {
			HashSet<Integer> ladderInstances = SceneryDao.getInstanceListByRoomIdAndSceneryId(roomId, ladderId);
			for (int tileId : ladderInstances) {
				image.setRGB(tileId % 250, tileId / 250, Color.BLACK.getRGB());
			}
		}
		
		// furnace
		HashSet<Integer> furnaceInstances = SceneryDao.getInstanceListByRoomIdAndSceneryId(roomId, 19);
		for (int tileId : furnaceInstances) {
			image.setRGB(tileId % 250, tileId / 250, Color.ORANGE.getRGB());
		}
		
		// obelisks
		for (int i = 21; i <= 28; ++i) {
			HashSet<Integer> instances = SceneryDao.getInstanceListByRoomIdAndSceneryId(roomId, i);
			for (int tileId : instances) {
				image.setRGB(tileId % 250, tileId / 250, Color.WHITE.getRGB());
			}
		}
		
		// ladders
		HashSet<Integer> instances = SceneryDao.getInstanceListByRoomIdAndSceneryId(roomId, 50);
		for (int tileId : instances) {
			image.setRGB(tileId % 250, tileId / 250, Color.BLACK.getRGB());
		}
		
		File outputfile = new File(String.format("D:\\github\\brackets\\img\\map_%d.png", roomId));
		ImageIO.write(image, "png", outputfile);
		
		images.put(roomId, image);
	}
	
	private static void drawStandardGroundTextureDataToImage(int roomId, BufferedImage image) throws IOException {
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
		
		for (GroundTextureDto dto : GroundTextureDao.getAllGroundTexturesByRoom(roomId)) {
			if (!standardGroundTextureSpriteMaps.contains(dto.getSpriteMapId()))			
				continue;
			
			for (int tileId : dto.getInstances()) {
				image.setRGB(tileId % 250, tileId / 250, (dto.getId() - 28) % 100 == 0 ? opposingAveragesBySpriteMapId.get(dto.getSpriteMapId()) : primaryAveragesBySpriteMapId.get(dto.getSpriteMapId()));
			}
		}
	}
	
	public static String getImage(int roomId) {
		if (!images.containsKey(roomId))
			return "";
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		try {
			ImageIO.write(images.get(roomId), "png", os);
			return Base64.getEncoder().encodeToString(os.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "";
	}
}
