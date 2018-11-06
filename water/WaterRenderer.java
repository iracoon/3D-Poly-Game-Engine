package water;

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

public class WaterRenderer {

	private RawModel quad;
	private WaterShader shader;
	private WaterFrameBuffers fbos;
	private float time = 0;
	private static final float WAVE_SPEED = 0.4f;

	public WaterRenderer(Loader loader, WaterShader shader, Matrix4f projectionMatrix, WaterFrameBuffers fbos) {
		this.shader = shader;
		this.fbos = fbos;
		shader.start();
		shader.connectTextureUnits();
		shader.loadProjectionMatrix(projectionMatrix);
		shader.stop();
		setUpVAO(loader);
	}

	public void render(List<WaterTile> water, Camera camera) {
		prepareRender(camera);	
		for (WaterTile tile : water) {
			Matrix4f modelMatrix = Maths.createTransformationMatrix(
					new Vector3f(tile.getX(), tile.getHeight(), tile.getZ()), 0, 0, 0,
					WaterTile.TILE_SIZE);
			shader.loadModelMatrix(modelMatrix);
			//GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, quad.getVertexCount());


			////
			GL11.glDrawElements(GL11.GL_TRIANGLES, quad.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
			////
		}
		unbind();
	}
	
	private void prepareRender(Camera camera){
		shader.start();
		/////
		updateTime();
		////
		shader.loadViewMatrix(camera);
		GL30.glBindVertexArray(quad.getVaoID());
		GL20.glEnableVertexAttribArray(0);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getReflectionTexture());
		GL13.glActiveTexture(GL13.GL_TEXTURE1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbos.getRefractionTexture());
	}
	
	private void unbind(){
		GL20.glDisableVertexAttribArray(0);
		GL30.glBindVertexArray(0);
		shader.stop();
	}

	private void setUpVAO(Loader loader) {
		// Just x and z vectex positions here, y is set to 0 in v.shader
    	//quad = loader.loadToVAO(positions, indices);

		final int VERTEX_COUNT = 100;
		final float SIZE = 0.02f;

		float[] positions = new float[VERTEX_COUNT * VERTEX_COUNT * 2];
		int pointer = 0;
		for(int i=0;i<VERTEX_COUNT;i++){
			for(int j=0;j<VERTEX_COUNT;j++){
				positions[pointer++] = j * SIZE;
				positions[pointer++] = i * SIZE;
			}
		}

		pointer = 0;
		int[] indices = new int[6*(VERTEX_COUNT-1)*(VERTEX_COUNT-1)];
		for(int gz=0;gz<VERTEX_COUNT-1;gz++){
			for(int gx=0;gx<VERTEX_COUNT-1;gx++){
				int topLeft = (gz*VERTEX_COUNT)+gx;
				int topRight = topLeft + 1;
				int bottomLeft = ((gz+1)*VERTEX_COUNT)+gx;
				int bottomRight = bottomLeft + 1;
				indices[pointer++] = topLeft;
				indices[pointer++] = bottomLeft;
				indices[pointer++] = topRight;
				indices[pointer++] = topRight;
				indices[pointer++] = bottomLeft;
				indices[pointer++] = bottomRight;
			}
		}

		quad = loader.loadData(positions, indices);
	}


	//ADD
	private void updateTime(){
		time+=DisplayManager.getFrameTimeSeconds()*WAVE_SPEED;
		time %= 1;
		shader.loadTime(time);
	}


}
