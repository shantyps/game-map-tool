# Shanty Game Map Tool

This tool allows one to draw polygons onto an image of the world map. After drawing a polygon, it will then prompt a 
window to export the polygon shapes into a Shanty enum MOD file that can then be used in the game. The program can 
also import existing Shanty enum MOD files to visualize them and use them as latch points for precision.

![Image of tool](https://i.imgur.com/qLFG1WB.png)

## Setup

The image must be loaded in `src/main/resources` with the name `game_map.png`. The bounding box coordinates are required
to be able to translate the image's coordinates to in-game coordinates. The bounding box in defined in Main#main.

```java
        // Specify the in-game coordinates for the four corners of the image
        double smallestX = 1024;
        double largestX = 1471;
        double smallestZ = 1600;
        double largestZ = 2111;
```