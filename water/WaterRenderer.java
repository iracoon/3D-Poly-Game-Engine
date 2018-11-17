package water;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import models.RawModel;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import renderEngine.DisplayManager;
import renderEngine.Loader;
import toolbox.Maths;
import entities.Camera;
import entities.Light;
import org.lwjgl.util.vector.Vector2f;

public class WaterRenderer {

	private RawModel quad;
	private WaterShader shader;
	private WaterFrameBuffers fbos;
	private float time = 0;
	private static final float WAVE_SPEED = 0.02f;

	public WaterRenderer(Loader loader, WaterShader shader, Matrix4f projectionMatrix, WaterFrameBuffers fbos) {
		this.shader = shader;
		this.fbos = fbos;
		shader.start();
		shader.connectTextureUnits();
		shader.loadProjectionMatrix(projectionMatrix);
		shader.stop();
		setUpVAO(loader);
	}

	public void render(List<WaterTile> water, Camera camera, Light sun) {
		prepareRender(camera, sun);
		for (WaterTile tile : water) {
			Matrix4f modelMatrix = Maths.createTransformationMatrix(
					new Vector3f(tile.getX(), tile.getHeight(), tile.getZ()), 0, 0, 0,
					WaterTile.TILE_SIZE);
			shader.loadModelMatrix(modelMatrix);
			GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, quad.getVertexCount());

				////
			//GL11.glDrawElements(GL11.GL_TRIANGLES, quad.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
			////
		}
		unbind();
	}
	
	private void prepareRender(Camera camera, Light sun){
		shader.start();
		shader.loadViewMatrix(camera);
		updateTime();
		shader.loadLight(sun);
		GL30.glBindVertexArray(quad.getVaoID());
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getReflectionTexture());
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getRefractionTexture());
		GL13.glActiveTexture(GL13.GL_TEXTURE2);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getRefractionDepthTexture());
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}
	
	private void unbind(){
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL30.glBindVertexArray(0);
		shader.stop();
	}

	private void updateTime(){
		time += DisplayManager.getFrameTimeSeconds()*WAVE_SPEED;
		shader.loadTime(time);
	}

	int positionPointer = 0;
	int indicatorPointer = 0;
	int gridCount = 20;
	int VERTICES_PER_SQUARE = 6;// 2 triangles, 3 vertices
	int totalVertexCount = gridCount * gridCount * VERTICES_PER_SQUARE;

	//vec2 , 2 coordinates per vertex
	float[] pos = new float[totalVertexCount * 2];

	//vec 4, 4 coordinates per vertex
	float[] indicats = new float[totalVertexCount * 4];

	private void setUpVAO(Loader loader) {

		for (int row = 0; row < gridCount; row++)
		{
			for (int col = 0; col < gridCount; col++)
			{
				Vector2f[] cornerPos = calculateCornerPositions(col, row);
				storeTriangle(cornerPos, pos, indicats, true);
				storeTriangle(cornerPos, pos, indicats, false);
			}
		}
		quad = loader.loadToVAO(pos, indicats, 2);
	}

	private void storeTriangle(Vector2f[] cornerPos, float[] positions, float[] indicators, boolean left) {
		int index0 = left ? 0 : 2;
		int index1 = 1;
		int index2 = left ? 2 : 3;

		packVertexData(cornerPos[index0], positions, indicators, getIndicators(index0, cornerPos, index1, index2));
		packVertexData(cornerPos[index1], positions, indicators, getIndicators(index1, cornerPos, index2, index0));
		packVertexData(cornerPos[index2], positions, indicators, getIndicators(index2, cornerPos, index0, index1));
	}

	public void packVertexData(Vector2f position, float[] pos, float[] indicats, float[] indicators) {
		pos[positionPointer++] = position.x * 0.068f;
		pos[positionPointer++] = position.y * 0.068f;
		for(int z = 0; z < 4; z++)
		{
			indicats[indicatorPointer++] = indicators[z] * 0.068f;
		}
	}


	private Vector2f[] calculateCornerPositions(float col, float row) {
		Vector2f[] vertices = new Vector2f[4];
		vertices[0] = new Vector2f(col, row);
		vertices[1] = new Vector2f(col, row + 1);
		vertices[2] = new Vector2f(col + 1, row);
		vertices[3] = new Vector2f(col + 1, row + 1);
		return vertices;
	}

	private float[] getIndicators(int currentVertex, Vector2f[] vertexPositions, int vertex1, int vertex2) {
		Vector2f currentVertexPos = vertexPositions[currentVertex];
		Vector2f vertex1Pos = vertexPositions[vertex1];
		Vector2f vertex2Pos = vertexPositions[vertex2];
		Vector2f offset1 = Vector2f.sub(vertex1Pos, currentVertexPos, null);
		Vector2f offset2 = Vector2f.sub(vertex2Pos, currentVertexPos, null);
		return new float[] { offset1.x, offset1.y, offset2.x, offset2.y };
	}

}
