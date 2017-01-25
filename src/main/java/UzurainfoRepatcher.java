import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class UzurainfoRepatcher {
    private final static int HEADER_HEIGHT = 50;
    private final static int BACKGROUND_COLOR = 0xFF3F3D3E;
    private final static int TILE_WIDTH = 195;
    private final static int TILE_HEIGHT = 412;

    public static void main(String[] args) throws IOException {
        Integer rowOption = null;
        Integer columnOption = null;
        Double ratioOption = null;
        File sourceFile = null;
        File targetFile = new File("a.png");
        boolean help = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-r":
                case "--row":
                    rowOption = Integer.parseInt(args[++i]);
                    if (rowOption < 1) {
                        System.err.println("column needs more than 0");
                        System.exit(1);
                    }
                    break;
                case "-c":
                case "--column":
                    columnOption = Integer.parseInt(args[++i]);
                    if (columnOption < 4) {
                        System.err.println("column needs more than 3");
                        System.exit(1);
                    }
                    break;
                case "-R":
                case "--ratio":
                    ratioOption = Double.parseDouble(args[++i]);
                    break;
                case "-o":
                    targetFile = new File(args[++i]);
                    break;
                case "-h":
                case "--help":
                    help = true;
                    break;
                default:
                    sourceFile = new File(arg);
                    break;
            }
        }
        if (help) {
            System.out.println("usage uzurainfo-repatcher <source> <option> [-o <target>]");
            System.out.println(" -r,--row <SIZE>       size of row");
            System.out.println(" -c,--column <SIZE>    size of column");
            System.out.println(" -R,--ratio <RATE>     ratio by width");
            System.exit(0);
        }
        if ((columnOption != null && rowOption != null) ||
                (rowOption != null && ratioOption != null)||
                (columnOption != null && ratioOption != null) ||
                (columnOption == null && rowOption == null && ratioOption == null)) {
            System.err.println("one of resize options is needed");
            System.exit(1);
        }
        if (sourceFile == null) {
            System.err.println("source image file is needed");
            System.exit(1);
        }
        BufferedImage sourceImage = ImageIO.read(sourceFile);
        BufferedImage[] items = split(sourceImage);
        int columnCount = 4;
        int rowCount = items.length / columnCount + (items.length % columnCount > 0 ? 1 : 0);
        if (columnOption != null) {
            columnCount = columnOption;
            rowCount = items.length / columnCount + (items.length % columnCount > 0 ? 1 : 0);
        } else if (rowOption != null) {
            rowCount = rowOption;
            columnCount = items.length / rowCount + (items.length % rowCount > 0 ? 1 : 0);
        } else {
            double minError = Double.MAX_VALUE;
            for (int i = 4; i <= items.length; i++) {
                int j = items.length / i + (items.length % i > 0 ? 1 : 0);
                double ratio = (double)(HEADER_HEIGHT + TILE_HEIGHT * j) / (i * TILE_WIDTH);
                if (Math.abs(ratio - ratioOption) < minError) {
                    minError = Math.abs(ratio - ratioOption);
                    columnCount = i;
                    rowCount = j;
                }
            }
        }
        BufferedImage target = new BufferedImage(TILE_WIDTH * columnCount, HEADER_HEIGHT + TILE_HEIGHT * rowCount, BufferedImage.TYPE_INT_RGB);
        Graphics targetGraphics = target.getGraphics();
        targetGraphics.setColor(new Color(BACKGROUND_COLOR));
        targetGraphics.drawImage(sourceImage, 0, 0, TILE_WIDTH * 4, HEADER_HEIGHT, 0, 0, TILE_WIDTH * 4, HEADER_HEIGHT, null);
        targetGraphics.fillRect(TILE_WIDTH * 4, 0, target.getWidth() - TILE_WIDTH  * 4, HEADER_HEIGHT);
        for (int y = 0; y < rowCount; y++) {
            for (int x = 0; x < columnCount; x++) {
                int i = x + y * columnCount;
                if (i < items.length) {
                    target.getGraphics().drawImage(items[i], x * TILE_WIDTH, HEADER_HEIGHT + y * TILE_HEIGHT, null);
                } else {
                    targetGraphics.fillRect(x * TILE_WIDTH, HEADER_HEIGHT + y * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
                }
            }
        }
        String formatName = "png";
        for (String i : ImageIO.getWriterFormatNames()) {
            if (targetFile.getName().endsWith("." + i)) {
                formatName = i;
                break;
            }
        }
        ImageIO.write(target, formatName, targetFile);
    }

    private static BufferedImage[] split(BufferedImage source) {
        int numY = (source.getHeight() - HEADER_HEIGHT) / TILE_HEIGHT;
        int numX = 6;
        BufferedImage[] tiles = new BufferedImage[numX * numY];
        for (int y = 0; y < numY; y++) {
            for (int x = 0; x < numX; x++) {
                int index = numX * y + x;
                tiles[index] = source.getSubimage(x * TILE_WIDTH, y * TILE_HEIGHT + HEADER_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
            }
        }
        int tilesToRemove = 0;
        for (int i = tiles.length -1; i >= 0; i--) {
            BufferedImage tile = tiles[i];
            int grayPixels = 0;
            for (int y = 0; y < TILE_HEIGHT; y++) {
                for (int x = 0; x < TILE_WIDTH; x++) {
                    if (tile.getRGB(x, y) == BACKGROUND_COLOR) {
                        grayPixels++;
                    }
                }
            }
            if (grayPixels / (float)(tile.getHeight() * tile.getWidth()) < 0.9) {
                break;
            } else {
                tiles[i] = null;
                tilesToRemove++;
            }
        }
        if (tilesToRemove == 0) {
            return tiles;
        } else {
            return Arrays.copyOf(tiles, tiles.length - tilesToRemove);
        }
    }
}
