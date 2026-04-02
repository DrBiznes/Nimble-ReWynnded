package com.jamino.nimblerewynnded;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.monster.Strider;
import org.lwjgl.glfw.GLFW;

public class NimbleRewynnded implements ClientModInitializer {
    private KeyMapping togglePerspectiveKey;
    private KeyMapping frontViewKey;
    private CameraType lastPerspective = CameraType.FIRST_PERSON;
    private CameraType preRidingPerspective = CameraType.FIRST_PERSON;
    private boolean wasInSpectator;
    private boolean wasRiding;
    private CameraType perspectiveBeforeTick = CameraType.FIRST_PERSON;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("nimblerewynnded:keys"));
        togglePerspectiveKey = new KeyMapping(
                "key.nimblerewynnded.toggleperspective",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F5,
                KeyMapping.Category.MISC
        );

        frontViewKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.nimblerewynnded.frontview",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                category
        ));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                perspectiveBeforeTick = client.options.getCameraType();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        if (client.player == null || client.gameMode == null) return;

        handlePerspectiveToggle(client, perspectiveBeforeTick);
        handleFrontViewToggle(client);
        handleCutscenePerspective(client);
        handleRidingPerspective(client);
    }

    private void handlePerspectiveToggle(Minecraft client, CameraType perspectiveBeforeTick) {
        if (client.player == null) return;
        if (togglePerspectiveKey.consumeClick()) {
            if (perspectiveBeforeTick == CameraType.FIRST_PERSON) {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            } else {
                client.options.setCameraType(CameraType.FIRST_PERSON);
            }
            lastPerspective = client.options.getCameraType();
            if (client.player.isPassenger()) {
                preRidingPerspective = lastPerspective;
            }
        }
    }

    private void handleFrontViewToggle(Minecraft client) {
        if (frontViewKey.isDown()) {
            if (client.options.getCameraType() != CameraType.THIRD_PERSON_FRONT) {
                lastPerspective = client.options.getCameraType();
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
        } else if (client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            client.options.setCameraType(lastPerspective);
        }
    }

    private void handleCutscenePerspective(Minecraft client) {
        if (client.gameMode == null) return;
        boolean isInSpectator = client.gameMode.getPlayerMode() == GameType.SPECTATOR;

        // Wynncraft-specific cutscene handling
        // This method detects when the player enters spectator mode, which is used for cutscenes in Wynncraft
        if (isInSpectator && !wasInSpectator) {
            lastPerspective = client.options.getCameraType();
            client.options.setCameraType(CameraType.FIRST_PERSON);
        } else if (!isInSpectator && wasInSpectator) {
            client.options.setCameraType(lastPerspective);
        }

        wasInSpectator = isInSpectator;
    }

    private void handleRidingPerspective(Minecraft client) {
        if (client.player == null) return;
        boolean isRiding = client.player.isPassenger();

        if (isRiding != wasRiding) {
            if (isRiding) {
                Entity vehicle = client.player.getVehicle();
                if (!wasInSpectator && shouldChangePerspective(vehicle)) {
                    preRidingPerspective = client.options.getCameraType();
                    if (preRidingPerspective == CameraType.FIRST_PERSON ||
                            preRidingPerspective == CameraType.THIRD_PERSON_FRONT) {
                        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    }
                }
            } else {
                if (!wasInSpectator) {
                    client.options.setCameraType(preRidingPerspective);
                }
            }
            wasRiding = isRiding;
        }
    }

    //Add new entity classes here if wynncraft adds custom mounts
    private boolean shouldChangePerspective(Entity vehicle) {
        return vehicle instanceof AbstractMinecart
                || vehicle instanceof Horse
                || vehicle instanceof Boat
                || vehicle instanceof Strider;
    }
}
