package ps.shanty.tool.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class Main extends JPanel {
    private BufferedImage mapImage;
    private BufferedImage zoomedImage;
    private JLabel imageLabel;
    private double smallestX;
    private double largestX;
    private double smallestZ;
    private double largestZ;
    private final List<Polygon> polygons;
    private final List<List<Line2D>> linesOfPolygons;
    private double scaleFactor = 1.0; // Zoom factor
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public Main() {
        polygons = new ArrayList<>();
        linesOfPolygons = new ArrayList<>();

        createNewPolygon();

        try {
            // Load the game's map image
            mapImage = loadImage("game_map.png");
            zoomedImage = mapImage;

            // Create a label to display the image
            imageLabel = new JLabel(new ImageIcon(zoomedImage)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;

                    getIcon().paintIcon(this, g, 0, 0);

                    // Draw the polygons
                    g2d.setColor(Color.RED);
                    for (Polygon polygon : polygons) {
                        if (polygon.npoints > 0) {
                            Polygon zoomedPolygon = zoomPolygon(polygon);
                            g2d.drawPolygon(zoomedPolygon);
                        }
                    }

                    // Draw the lines
                    g2d.setColor(Color.GREEN);
                    for (Line2D line : getCurrentLines()) {
                        Line2D zoomedLine = zoomLine(line);
                        g2d.draw(zoomedLine);
                    }
                }
            };

            JScrollPane scrollPane = new JScrollPane(imageLabel);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            screenSize.setSize(screenSize.getWidth() * 0.8, screenSize.getHeight() * 0.8);
            scrollPane.setPreferredSize(screenSize);

            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            add(scrollPane);

            imageLabel.addMouseWheelListener(new MouseAdapter() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    int rotation = e.getWheelRotation();
                    if (rotation < 0) {
                        // Zoom in
                        scaleFactor *= 1.1;
                    } else {
                        // Zoom out
                        scaleFactor /= 1.1;
                    }

                    // Update the zoomed image
                    int newWidth = (int) (mapImage.getWidth() * scaleFactor);
                    int newHeight = (int) (mapImage.getHeight() * scaleFactor);
                    zoomedImage = getScaledImage(mapImage, newWidth, newHeight);

                    imageLabel.setIcon(new ImageIcon(zoomedImage));

                    scrollPane.revalidate();
                    scrollPane.repaint();
                }
            });

            imageLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int x = (int) (e.getX() / scaleFactor);
                    int y = (int) (e.getY() / scaleFactor);

                    Point latchPoint = findLatchPoint(x, y, 5);

                    if (latchPoint != null) {
                        // Latch onto the nearby point
                        x = latchPoint.x;
                        y = latchPoint.y;
                    }

                    if (SwingUtilities.isRightMouseButton(e)) {
                        // Find the closest point to the clicked position and remove it
                        Polygon currentPolygon = getCurrentPolygon();
                        int closestIndex = findClosestPointIndex(x, y, currentPolygon);
                        if (closestIndex >= 0) {
                            currentPolygon.xpoints = removeElement(currentPolygon.xpoints, closestIndex);
                            currentPolygon.ypoints = removeElement(currentPolygon.ypoints, closestIndex);
                            currentPolygon.npoints -= 1;
                            getCurrentLines().remove(closestIndex - 1);

                            if (currentPolygon.npoints == 1) {
                                currentPolygon.npoints = 0;
                                currentPolygon.xpoints = new int[] {};
                                currentPolygon.ypoints = new int[] {};
                                getCurrentLines().clear();
                            }
                        }
                    } else {
                        String gameCoords = estimateInGameCoordinate(x, y, mapImage.getWidth(), mapImage.getHeight());

                        System.out.println("Clicked point: (" + x + ", " + y + ")");
                        System.out.println("Game coordinates: (" + gameCoords + ")");

                        getCurrentPolygon().addPoint(x, y);

                        if (getCurrentPolygon().npoints >= 2) {
                            int prevX = getCurrentPolygon().xpoints[getCurrentPolygon().npoints - 2];
                            int prevY = getCurrentPolygon().ypoints[getCurrentPolygon().npoints - 2];
                            getCurrentLines().add(new Line2D.Double(prevX, prevY, x, y));

                            if (isPolygonFinished()) {
                                exportPolygonInfo();
                                createNewPolygon();
                            }
                        }
                    }

                    repaint();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper method to remove an element from an array
    private int[] removeElement(int[] array, int index) {
        int[] newArray = new int[array.length - 1];
        System.arraycopy(array, 0, newArray, 0, index);
        System.arraycopy(array, index + 1, newArray, index, array.length - index - 1);
        return newArray;
    }

    // Helper method to find the index of the closest point to the clicked position
    private int findClosestPointIndex(int x, int y, Polygon polygon) {
        int closestIndex = -1;
        double smallestDistance = Double.MAX_VALUE;
        for (int i = 0; i < polygon.npoints; i++) {
            int px = polygon.xpoints[i];
            int py = polygon.ypoints[i];
            double distance = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py));
            if (distance < smallestDistance) {
                smallestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem importMenuItem = new JMenuItem("Import");

        importMenuItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            int result = fileChooser.showOpenDialog(Main.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                for (File file : selectedFiles) {
                    System.out.println("Importing file: " + file.getAbsolutePath());
                    try {
                        AreaEnum areaEnum = mapper.readValue(file, AreaEnum.class);
                        createNewPolygon();
                        for (String coordinate : areaEnum.getValues().values()) {
                            int[] coords = estimateImageCoordinate(coordinate, mapImage.getWidth(), mapImage.getHeight());
                            int x = coords[0];
                            int y = coords[1];
                            getCurrentPolygon().addPoint(x, y);
                            if (getCurrentPolygon().npoints >= 2) {
                                int prevX = getCurrentPolygon().xpoints[getCurrentPolygon().npoints - 2];
                                int prevY = getCurrentPolygon().ypoints[getCurrentPolygon().npoints - 2];
                                getCurrentLines().add(new Line2D.Double(prevX, prevY, x, y));
                            }
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                createNewPolygon();
                repaint();
            }
        });

        fileMenu.add(importMenuItem);
        menuBar.add(fileMenu);

        return menuBar;
    }

    private Line2D zoomLine(Line2D line) {
        return new Line2D.Double(line.getX1() * scaleFactor,
                line.getY1() * scaleFactor,
                line.getX2() * scaleFactor,
                line.getY2() * scaleFactor);
    }

    private Polygon zoomPolygon(Polygon polygon) {
        Polygon zoomedPolygon = new Polygon();
        for (int i = 0; i < polygon.npoints; i++) {
            int x = (int) (polygon.xpoints[i] * scaleFactor);
            int y = (int) (polygon.ypoints[i] * scaleFactor);
            zoomedPolygon.addPoint(x, y);
        }
        return zoomedPolygon;
    }


    private void exportPolygonInfo() {
        String userInput = JOptionPane.showInputDialog(null, "What's the name of the area?", "Export Area", JOptionPane.PLAIN_MESSAGE);
        if (userInput != null) {
            Polygon polygon = getCurrentPolygon();
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < polygon.npoints; i++) {
                int px = polygon.xpoints[i];
                int py = polygon.ypoints[i];

                String gameCoords = estimateInGameCoordinate(px, py, mapImage.getWidth(), mapImage.getHeight());
                values.put(i + "", gameCoords);
            }
            AreaEnum areaEnum = new AreaEnum();
            areaEnum.setInputType("int");
            areaEnum.setOutputType("coordinate");
            areaEnum.setDefaultValue(-1);
            areaEnum.setValues(values);

            try {
                mapper.writeValue(new File("exports/" + userInput + ".enum"), areaEnum);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("No input provided");
        }
    }

    private boolean isPolygonFinished() {
        Point2D firstPoint = getCurrentLines().get(0).getP1();
        Point2D latestPoint = getCurrentLines().get(getCurrentLines().size() - 1).getP2();
        return firstPoint.equals(latestPoint);
    }

    private void createNewPolygon() {
        polygons.add(new Polygon());
        linesOfPolygons.add(new ArrayList<>());
    }

    private Polygon getCurrentPolygon() {
        return polygons.get(polygons.size() - 1);
    }

    private List<Line2D> getCurrentLines() {
        return linesOfPolygons.get(linesOfPolygons.size() - 1);
    }

    private Point findLatchPoint(int x, int y, int maxDistance) {
        Point closestPoint = null;
        double smallestDistance = Double.MAX_VALUE;
        for (Polygon polygon : polygons) {
            for (int i = 0; i < polygon.npoints; i++) {
                int px = polygon.xpoints[i];
                int py = polygon.ypoints[i];
                double distance = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py));
                if (distance <= maxDistance && distance < smallestDistance) {
                    smallestDistance = distance;
                    closestPoint = new Point(px, py);
                }
            }
        }
        return closestPoint;
    }

    private BufferedImage loadImage(String imagePath) throws IOException {
        return ImageIO.read(Main.class.getClassLoader().getResourceAsStream(imagePath));
    }

    private BufferedImage getScaledImage(BufferedImage originalImage, int width, int height) {
        Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();
        return newImage;
    }

    public String estimateInGameCoordinate(double clickedX, double clickedY, double imageWidth, double imageHeight) {
        double xRatio = clickedX / imageWidth;
        double zRatio = clickedY / imageHeight;

        double diffX = (largestX - smallestX) * xRatio;
        double diffZ = (smallestZ - largestZ) * zRatio;

        long absX = Math.round(smallestX + diffX);
        long absZ = Math.round(largestZ + diffZ);

        long mapSquareX = absX >> 6;
        long mapSquareZ = absZ >> 6;
        long tileX = absX & 63;
        long tileZ = absZ & 63;
        return "0_" + mapSquareX + "_" + mapSquareZ + "_" + tileX + "_" + tileZ;
    }

    public int[] estimateImageCoordinate(String gameCoordinate, double imageWidth, double imageHeight) {
        String[] parts = gameCoordinate.split("_");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid game coordinate format.");
        }

        long mapSquareX = Long.parseLong(parts[1]);
        long mapSquareZ = Long.parseLong(parts[2]);
        long tileX = Long.parseLong(parts[3]);
        long tileZ = Long.parseLong(parts[4]);

        long absX = (mapSquareX << 6) + tileX;
        long absZ = (mapSquareZ << 6) + tileZ;

        double xRatio = (absX - smallestX) / (largestX - smallestX);
        double yRatio = (absZ - largestZ) / (smallestZ - largestZ);

        long clickedX = Math.round(xRatio * imageWidth);
        long clickedY = Math.round(yRatio * imageHeight);

        return new int[]{(int) clickedX, (int) clickedY};
    }

    public static void main(String[] args) {
        // Specify the in-game coordinates for the four corners of the image
        double smallestX = 1024;
        double largestX = 1471;
        double smallestZ = 1600;
        double largestZ = 2111;

        SwingUtilities.invokeLater(() -> {
            Main converter = new Main();
            converter.setCornerCoordinates(smallestX, largestX, smallestZ, largestZ);

            JFrame frame = new JFrame("Image Coordinate Converter");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setJMenuBar(converter.createMenuBar());
            frame.getContentPane().add(converter);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public void setCornerCoordinates(double smallestX, double largestX, double smallestZ, double largestZ) {
        this.smallestX = smallestX;
        this.largestX = largestX;
        this.smallestZ = smallestZ;
        this.largestZ = largestZ;
    }
}
