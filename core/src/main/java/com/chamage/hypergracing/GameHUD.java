package com.chamage.hypergracing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class GameHUD implements Disposable {
    private Stage stage;
    private Skin skin;
    private Label speedLabel;
    private Label gearLabel;

    public GameHUD() {
        skin = new Skin(Gdx.files.internal("ui/uiskin.json")); //

        stage = new Stage(new ScreenViewport());

        speedLabel = new Label("Speed: 0", skin);
        gearLabel = new Label("Gear: Netural", skin);

        Table table = new Table();
        table.top().left();
        table.setFillParent(true);
        table.add(speedLabel).pad(10);
        table.add(gearLabel).pad(10);

        stage.addActor(table);
    }

    public void updateSpeed(float speed) {
        speedLabel.setText(String.format("Speed: %.0f", speed));
    }

    public void updateGear(GearState gear) {
        gearLabel.setText(String.format("Gear: %s", gear));
    }

    public void render(float delta) {
        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public Stage getStage() {
        return stage;
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
    }
}
