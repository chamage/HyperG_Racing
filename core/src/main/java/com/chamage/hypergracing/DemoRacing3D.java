package com.chamage.hypergracing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class DemoRacing3D implements Screen {

    // Main stuff
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Model carModel;
    private ModelInstance carInstance;
    private Environment environment;

    // Car properties
    private Vector3 carPosition;
    private Quaternion carRotation;
    private float carSpeed = 0f;
    private final float MAX_SPEED = 20f;
    private final float ACCELERATION = 15f;
    private final float DECELERATION = 20f;
    private final float TURN_SPEED = 60f;

    // Camera properties
    private final Vector3 cameraOffset = new Vector3(0f, 4f, -8f);

    // Ground properties
    private Model tileModel;
    private Material lightGrayMaterial;
    private ModelInstance[][] visibleTileInstances;
    private final int VISIBLE_TILES_RADIUS = 15;
    private final float TILE_SIZE = 5f;
    private final float TILE_THICKNESS = 0.1f;
    private int lastProcessedCenterTileX = Integer.MIN_VALUE;
    private int lastProcessedCenterTileZ = Integer.MIN_VALUE;

    @Override
    public void show() {
        modelBatch = new ModelBatch();
        ModelBuilder modelBuilder = new ModelBuilder();

        camera = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = (VISIBLE_TILES_RADIUS + 5) * TILE_SIZE;

        Material carMaterial = new Material(ColorAttribute.createDiffuse(Color.RED));
        carModel = modelBuilder.createBox(1f, 0.5f, 2.5f, carMaterial, Usage.Position | Usage.Normal);
        carInstance = new ModelInstance(carModel);

        carPosition = new Vector3(0, 0.5f / 2.0f, 0);
        carRotation = new Quaternion();
        carInstance.transform.setToTranslation(carPosition).rotate(carRotation);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        lightGrayMaterial = new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY));

        tileModel = modelBuilder.createBox(TILE_SIZE, TILE_THICKNESS, TILE_SIZE, lightGrayMaterial, Usage.Position | Usage.Normal);

        int gridSize = VISIBLE_TILES_RADIUS * 2 + 1;
        visibleTileInstances = new ModelInstance[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                visibleTileInstances[i][j] = new ModelInstance(tileModel);
            }
        }
        updateInfiniteGround(true);
    }

    private void updateInfiniteGround(boolean forceUpdate) {
        int centerWorldTileX = (int) Math.round(carPosition.x / TILE_SIZE);
        int centerWorldTileZ = (int) Math.round(carPosition.z / TILE_SIZE);

        if (!forceUpdate && centerWorldTileX == lastProcessedCenterTileX && centerWorldTileZ == lastProcessedCenterTileZ) {
            return;
        }

        lastProcessedCenterTileX = centerWorldTileX;
        lastProcessedCenterTileZ = centerWorldTileZ;

        float groundTileModelCenterY = -TILE_THICKNESS / 2.0f;

        int gridSize = VISIBLE_TILES_RADIUS * 2 + 1;

        for (int screenX = 0; screenX < gridSize; screenX++) {
            for (int screenZ = 0; screenZ < gridSize; screenZ++) {
                int worldTileX = centerWorldTileX + screenX - VISIBLE_TILES_RADIUS;
                int worldTileZ = centerWorldTileZ + screenZ - VISIBLE_TILES_RADIUS;

                ModelInstance tileInstance = visibleTileInstances[screenX][screenZ];

                float tileCenterX = worldTileX * TILE_SIZE;
                float tileCenterZ = worldTileZ * TILE_SIZE;
                tileInstance.transform.setToTranslation(tileCenterX, groundTileModelCenterY, tileCenterZ);

                boolean isLightTile = (Math.floorMod(worldTileX, 2) + Math.floorMod(worldTileZ, 2)) % 2 == 0;

                ColorAttribute colorAttribute = (ColorAttribute) tileInstance.materials.get(0).get(ColorAttribute.Diffuse);
                if (colorAttribute != null) {
                    if (isLightTile) {
                        colorAttribute.color.set(Color.LIGHT_GRAY);
                    } else {
                        colorAttribute.color.set(Color.DARK_GRAY);
                    }
                }
            }
        }
    }


    @Override
    public void render(float delta) {
        handleInput(delta);
        updateCar(delta);
        updateInfiniteGround(false);
        updateCamera();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.3f, 0.5f, 0.8f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);

        int gridSize = VISIBLE_TILES_RADIUS * 2 + 1;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                modelBatch.render(visibleTileInstances[i][j], environment);
            }
        }

        modelBatch.render(carInstance, environment);
        modelBatch.end();
    }

    private void handleInput(float deltaTime) {
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            carSpeed += ACCELERATION * deltaTime;
            if (carSpeed > MAX_SPEED) carSpeed = MAX_SPEED;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            carSpeed -= ACCELERATION * deltaTime * 1.5f;
            if (carSpeed < -MAX_SPEED / 2f) carSpeed = -MAX_SPEED / 2f;
        } else {
            if (carSpeed > 0) {
                carSpeed -= DECELERATION * deltaTime;
                if (carSpeed < 0) carSpeed = 0;
            } else if (carSpeed < 0) {
                carSpeed += DECELERATION * deltaTime;
                if (carSpeed > 0) carSpeed = 0;
            }
        }

        if (Math.abs(carSpeed) > 0.1f) {
            float currentTurnRatio = Math.min(1f, Math.abs(carSpeed) / (MAX_SPEED * 0.5f));
            float actualTurnSpeed = TURN_SPEED * currentTurnRatio;

            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                carRotation.mul(new Quaternion(Vector3.Y, actualTurnSpeed * deltaTime));
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                carRotation.mul(new Quaternion(Vector3.Y, -actualTurnSpeed * deltaTime));
            }
        }
    }

    private void updateCar(float deltaTime) {
        Vector3 forwardDirection = new Vector3(0, 0, 1);
        carRotation.transform(forwardDirection);
        forwardDirection.nor();

        Vector3 velocity = forwardDirection.scl(carSpeed * deltaTime);
        carPosition.add(velocity);
        carPosition.y = 0.5f / 2.0f;

        carInstance.transform.setToTranslation(carPosition).rotate(carRotation);
    }

    private void updateCamera() {
        Vector3 desiredPosition = new Vector3(cameraOffset);
        Quaternion carYRotation = new Quaternion().setFromAxis(Vector3.Y, carRotation.getAngleAround(Vector3.Y));
        carYRotation.transform(desiredPosition);
        desiredPosition.add(carPosition);

        camera.position.set(desiredPosition);
        Vector3 lookAtPoint = new Vector3(carPosition.x, carPosition.y + 0.2f, carPosition.z);
        camera.lookAt(lookAtPoint);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        if (camera != null) {
            camera.viewportWidth = width;
            camera.viewportHeight = height;
            camera.update();
        }
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        modelBatch.dispose();
        carModel.dispose();
        if (tileModel != null) tileModel.dispose();
    }
}
