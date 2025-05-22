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
    private final Vector3 cameraOffset = new Vector3(0f, 5f, -10f);

    // Ground properties
    private Array<ModelInstance> groundTiles;
    private Model lightGrayTileModel;
    private Model darkGrayTileModel;
    private final float TILE_SIZE = 5f;
    private final int TILES_COUNT_X = 20;
    private final int TILES_COUNT_Z = 20;
    private final float TILE_THICKNESS = 0.1f;

    @Override
    public void show() {
        modelBatch = new ModelBatch();
        ModelBuilder modelBuilder = new ModelBuilder();

        // 1. Setting up a 3D camera
        camera = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = 300f;

        // 2. Creating the model of the car
        Material carMaterial = new Material(ColorAttribute.createDiffuse(Color.RED));
        carModel = modelBuilder.createBox(1f, 0.5f, 2.5f, carMaterial, Usage.Position | Usage.Normal);
        carInstance = new ModelInstance(carModel);

        carPosition = new Vector3(0, 0.5f / 2.0f, 0);
        carRotation = new Quaternion();
        carInstance.transform.setToTranslation(carPosition).rotate(carRotation);

        // 3. Setting up the environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // 4. Creating the tiles
        groundTiles = new Array<>();
        Material lightGrayMaterial = new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY));
        Material darkGrayMaterial = new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY));

        lightGrayTileModel = modelBuilder.createBox(TILE_SIZE, TILE_THICKNESS, TILE_SIZE, lightGrayMaterial, Usage.Position | Usage.Normal);
        darkGrayTileModel = modelBuilder.createBox(TILE_SIZE, TILE_THICKNESS, TILE_SIZE, darkGrayMaterial, Usage.Position | Usage.Normal);

        float groundTileCenterY = -TILE_THICKNESS / 2.0f;

        for (int i = 0; i < TILES_COUNT_X; i++) {
            for (int j = 0; j < TILES_COUNT_Z; j++) {
                ModelInstance tileInstance;
                if ((i + j) % 2 == 0) {
                    tileInstance = new ModelInstance(lightGrayTileModel);
                } else {
                    tileInstance = new ModelInstance(darkGrayTileModel);
                }

                float xPos = (i - TILES_COUNT_X / 2.0f + 0.5f) * TILE_SIZE;
                float zPos = (j - TILES_COUNT_Z / 2.0f + 0.5f) * TILE_SIZE;

                tileInstance.transform.setToTranslation(xPos, groundTileCenterY, zPos);
                groundTiles.add(tileInstance);
            }
        }
    }

    @Override
    public void render(float delta) {
        handleInput(delta);
        updateCar(delta);
        updateCamera();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.3f, 0.5f, 0.8f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        for (ModelInstance tile : groundTiles) {
            modelBatch.render(tile, environment);
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
        if (lightGrayTileModel != null) lightGrayTileModel.dispose();
        if (darkGrayTileModel != null) darkGrayTileModel.dispose();
        if (groundTiles != null) groundTiles.clear();
    }
}
