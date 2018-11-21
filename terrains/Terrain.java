package terrains;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import models.RawModel;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import renderEngine.Loader;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.Maths;

public class Terrain {

	private static final float SIZE = 150;
	private static final float MAX_HEIGHT = 40;
	private static final float MAX_PIXEL_COLOUR = 256 * 256 * 256;

	private float x;
	private float z;
	private RawModel model;
	private TerrainTexturePack texturePack;
	private TerrainTexture blendMap;

	private float[][] heights;

	public Terrain(int gridX, int gridZ, Loader loader, TerrainTexturePack texturePack,
				   TerrainTexture blendMap, String heightMap) {
		this.texturePack = texturePack;
		this.blendMap = blendMap;
		this.x = gridX * SIZE;
		this.z = gridZ * SIZE;
		this.model = generateTerrain(loader, heightMap);
	}

	public float getX() {
		return x;
	}

	public float getZ() {
		return z;
	}

	public RawModel getModel() {
		return model;
	}

	public TerrainTexturePack getTexturePack() {
		return texturePack;
	}

	public TerrainTexture getBlendMap() {
		return blendMap;
	}

	public float getHeightOfTerrain(float worldX, float worldZ) {
		float terrainX = worldX - this.x;
		float terrainZ = worldZ - this.z;
		float gridSquareSize = SIZE / ((float) heights.length - 1);
		int gridX = (int) Math.floor(terrainX / gridSquareSize);
		int gridZ = (int) Math.floor(terrainZ / gridSquareSize);

		if(gridX >= heights.length - 1 || gridZ >= heights.length - 1 || gridX < 0 || gridZ < 0) {
			return 0;
		}

		float xCoord = (terrainX % gridSquareSize)/gridSquareSize;
		float zCoord = (terrainZ % gridSquareSize)/gridSquareSize;
		float answer;

		if (xCoord <= (1-zCoord)) {
			answer = Maths.barryCentric(new Vector3f(0, heights[gridX][gridZ], 0), new Vector3f(1,
					heights[gridX + 1][gridZ], 0), new Vector3f(0,
					heights[gridX][gridZ + 1], 1), new Vector2f(xCoord, zCoord));
		} else {
			answer = Maths.barryCentric(new Vector3f(1, heights[gridX + 1][gridZ], 0), new Vector3f(1,
					heights[gridX + 1][gridZ + 1], 1), new Vector3f(0,
					heights[gridX][gridZ + 1], 1), new Vector2f(xCoord, zCoord));
		}

		return answer;
	}

	///////////////////////// Special terrain

	BufferedImage image = null;
	int vertPointer = 0;
	int normPointer = 0;
	int texPointer = 0;

	private RawModel generateTerrain(Loader loader, String heightMap) {


		try {
			image = ImageIO.read(new File("res/" + heightMap + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		int VERTEX_COUNT = image.getHeight();
		heights = new float[VERTEX_COUNT][VERTEX_COUNT];
		int vertexCount = calculateVertexCount(heights.length);
		float[] vertices = new float[vertexCount * 3];
		float[] normals = new float[vertexCount * 3];
		float[] textureCoords = new float[vertexCount * 2];

		///////////////// adds heights to array
		for (int row = 0; row < heights.length - 1; row++) {
			for (int col = 0; col < heights[row].length - 1; col++) {
				float height = getHeight(row, col, image);
				heights[row][col] = height;
				heights[row][col] = height;
			}
		}
		/////////////////////
		for (int row = 0; row < heights.length - 1; row++) {
			for (int col = 0; col < heights[row].length - 1; col++) {
				storeGridSquare(col, row, heights, vertices, normals, textureCoords);
			}
		}
		return loader.loadToVAO(vertices, textureCoords, normals);
	}

	//////////////
	private void storeGridSquare(int col, int row, float[][] heights,float[] vertices, float[] normals, float[] textureCoords) {
		Vector3f[] cornerPos = calculateCornerPositions(col, row, heights);
		boolean rightHanded = col % 2 != row % 2;

		if(rightHanded)
		{
			Vector3f normalTopLeft = calcNormal(cornerPos[0], cornerPos[1], cornerPos[2]);
			Vector3f normalBottomRight = calcNormal(cornerPos[2], cornerPos[1], cornerPos[3]);
			storeTriangle(cornerPos, normalTopLeft, 0, 1, 2, vertices, normals, textureCoords, row, col);
			storeTriangle(cornerPos, normalBottomRight, 2, 1, 3, vertices, normals, textureCoords, row, col);
		}
		else
		{
			Vector3f normalTopLeft = calcNormal(cornerPos[0], cornerPos[1], cornerPos[3]);
			Vector3f normalBottomRight = calcNormal(cornerPos[2], cornerPos[0], cornerPos[3]);
			storeTriangle(cornerPos, normalTopLeft, 0, 1, 3, vertices, normals, textureCoords, row, col);
			storeTriangle(cornerPos, normalBottomRight, 2, 0, 3, vertices, normals, textureCoords, row, col);
		}
	}

	private void storeTriangle(Vector3f[] cornerPos, Vector3f normal, int index0, int index1,
							   int index2, float[] vertices, float[] normals, float[] textureCoords, int row, int col){
		paackVertexData(cornerPos[index0], normal, vertices, normals, textureCoords, row, col);
		paackVertexData(cornerPos[index1], normal, vertices, normals, textureCoords, row, col);
		paackVertexData(cornerPos[index2], normal, vertices, normals, textureCoords, row, col);
	}

	public void paackVertexData(Vector3f aVert, Vector3f theNormal,
								float[] vertices, float[] normals, float[] textureCoords, int row, int col) {
		//vec3 position
		vertices[vertPointer++] = aVert.x ;
		vertices[vertPointer++] = aVert.y ;
		vertices[vertPointer++] = aVert.z ;

		//vec3 normal
		normals[normPointer++] = theNormal.x ;
		normals[normPointer++] = theNormal.y;
		normals[normPointer++] = theNormal.z;

		//vec2 texCoords
		textureCoords[texPointer++] = aVert.x/ (heights.length);
		textureCoords[texPointer++] = aVert.z/ (heights.length);
	}

	private Vector3f[] calculateCornerPositions(int col, int row, float[][] heights) {
		Vector3f[] vertices = new Vector3f[4];
		vertices[0] = new Vector3f(col, heights[row][col], row);
		vertices[1] = new Vector3f(col, heights[row + 1][col], row + 1);
		vertices[2] = new Vector3f(col + 1, heights[row][col + 1], row);
		vertices[3] = new Vector3f(col + 1, heights[row + 1][col + 1], row + 1);
		return vertices;
	}

	private int calculateVertexCount(int vertexLength) {
		int gridSquareLength = vertexLength - 1;
		int totalGridSquares = gridSquareLength * gridSquareLength;
		return totalGridSquares * 2 * 3;// 2 triangles with 3 verts each
	}

	public static Vector3f calcNormal(Vector3f vertex0, Vector3f vertex1, Vector3f vertex2) {
		Vector3f tangentA = Vector3f.sub(vertex1, vertex0, null);
		Vector3f tangentB = Vector3f.sub(vertex2, vertex0, null);
		Vector3f normal = Vector3f.cross(tangentA, tangentB, null);
		normal.normalise();
		return normal;
	}
	/////////////////

	private float getHeight(int x, int z, BufferedImage image){
		if(x<0 || x>=image.getHeight() || z<0 || z>=image.getHeight()){
			return 0;
		}
		float height = image.getRGB(x, z);
		height += MAX_PIXEL_COLOUR/2f;
		height /= MAX_PIXEL_COLOUR/2f;
		height *= MAX_HEIGHT;
		return height;
	}


}