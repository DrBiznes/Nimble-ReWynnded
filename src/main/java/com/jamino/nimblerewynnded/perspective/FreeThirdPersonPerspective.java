package com.jamino.nimblerewynnded.perspective;

import com.jamino.nimblerewynnded.mixin.ClientInputAccessor;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import io.github.leawind.perspectiveapi.api.PerspectiveMath;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * Free third person: the camera orbits the player and the mouse only turns the camera.
 * The player turns towards the direction they move in, and movement keys are relative to the camera.
 * The scroll wheel zooms (distance and FOV together).
 * <p>
 * Adapted from Leawind's Perspective API Demo (MIT).
 * Loaded by Perspective API through {@link java.util.ServiceLoader}.
 */
@PerspectiveInfo.Declaration(
        id = FreeThirdPersonPerspective.ID,
        priority = 10,
        switchable = false,
        baseType = PerspectiveBehavior.BaseType.THIRD_PERSON_BACK)
public final class FreeThirdPersonPerspective implements PerspectiveBehavior {
    public static final String ID = "nimblerewynnded.free_third_person";

    private static final float MOUSE_ROTATION_SCALE = 0.15f;
    private static final float DEFAULT_FOV_DEG = 70.0f;
    private static final double DEFAULT_DISTANCE = 4.0;
    private static final double ZOOM_SCROLL_BASE = 1.1487;
    private static final double ZOOM_HALFLIFE_MS = 100;

    private static FreeThirdPersonPerspective instance;

    private final Vector3d position = new Vector3d();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector2f eulerDeg = new Vector2f();

    private double fovHalfTan = halfHeight(DEFAULT_DISTANCE, DEFAULT_FOV_DEG) / 4.0;
    private double fovHalfTanTarget = fovHalfTan;
    private long lastSmoothTime;
    private double frustumHalfHeight = halfHeight(DEFAULT_DISTANCE, DEFAULT_FOV_DEG);
    private boolean needInit = true;

    public FreeThirdPersonPerspective() {
        instance = this;
    }

    private static boolean active() {
        return instance != null && PerspectiveAPI.isCurrent(ID);
    }

    /** Returns true if the mouse movement was used to turn the camera instead of the player. */
    public static boolean onMouseTurn(double dx, double dy) {
        if (!active()) return false;
        instance.eulerDeg.y += (float) dx * MOUSE_ROTATION_SCALE;
        instance.eulerDeg.x = Math.max(-90f, Math.min(90f, instance.eulerDeg.x + (float) dy * MOUSE_ROTATION_SCALE));
        return true;
    }

    /** Returns true if the scroll was used to zoom instead of changing the hotbar slot. */
    public static boolean onMouseScroll(double yOffset) {
        if (!active() || Minecraft.getInstance().isPaused()) return false;
        instance.fovHalfTanTarget *= Math.pow(ZOOM_SCROLL_BASE, -yOffset);
        return true;
    }

    /** Called right after the movement keys are read; makes them relative to the camera. */
    public static void onInputTick(LocalPlayer player, ClientInput input) {
        if (!active()) return;
        Vec2 move = input.getMoveVector();
        Vector2f impulse = new Vector2f(move.x, move.y);
        instance.remapMovement(player, impulse);
        ((ClientInputAccessor) input).setMoveVector(new Vec2(impulse.x, impulse.y));
    }

    private void remapMovement(LocalPlayer player, Vector2f impulse) {
        Vec2 playerRotationDeg = player.getRotationVector();
        Quaternionf playerRotation = PerspectiveMath.eulerDegToQuat(
                new Vector2f(playerRotationDeg.x, playerRotationDeg.y), new Quaternionf());
        Vector3f moveVector = new Vector3f(-impulse.x, 0, -impulse.y);
        rotation.transform(moveVector, moveVector);
        playerRotation.transformInverse(moveVector, moveVector);

        var movement = player.getDeltaMovement();
        if (movement.lengthSqr() > 0.01f) {
            var orientation = PerspectiveMath.directionToEulerDeg(movement.toVector3f(), new Vector2f());
            player.setYRot(orientation.y);
        }

        impulse.x = -moveVector.x;
        impulse.y = -moveVector.z;
    }

    @Override
    public void init() {
        // Nothing to register; the mixins call the static hooks above.
    }

    @Override
    public void onActivate() {
        needInit = true;
    }

    @Override
    public void computeCameraState(PerspectiveState.Mutable state, PerspectiveContext context) {
        Entity entity = context.cameraEntity();
        if (entity == null) return;

        frustumHalfHeight = halfHeight(4 * entity.getBoundingBox().getSize(), DEFAULT_FOV_DEG);
        var eyePos = entity.getEyePosition(context.partialTicks());

        if (needInit) {
            Vec2 rotVec = entity.getRotationVector();
            eulerDeg.set(rotVec.x, rotVec.y);
            needInit = false;
        }

        smoothZoom();

        PerspectiveMath.eulerDegToQuat(eulerDeg, rotation);
        var backward = PerspectiveMath.getBackward(rotation, new Vector3f());
        position.set(eyePos.x, eyePos.y, eyePos.z).add(backward.mul((float) (frustumHalfHeight / fovHalfTan)));

        // Re-derive the rotation from the view vector so the camera always faces the player
        Vector3f viewVectorToEntity = new Vector3f(
                (float) (eyePos.x - position.x),
                (float) (eyePos.y - position.y),
                (float) (eyePos.z - position.z));
        PerspectiveMath.directionToQuat(viewVectorToEntity, rotation);

        state.position().set(position);
        state.rotation().set(rotation);
        state.setFovDeg((float) Math.toDegrees(2 * Math.atan(fovHalfTan)));
    }

    private void smoothZoom() {
        long now = System.currentTimeMillis();
        double decay = Math.pow(0.5, (now - lastSmoothTime) / ZOOM_HALFLIFE_MS);
        fovHalfTan += (fovHalfTanTarget - fovHalfTan) * (1 - decay);
        lastSmoothTime = now;
    }

    private static double halfHeight(double distance, float fovDeg) {
        return distance * Math.tan(Math.toRadians(fovDeg) / 2);
    }
}
