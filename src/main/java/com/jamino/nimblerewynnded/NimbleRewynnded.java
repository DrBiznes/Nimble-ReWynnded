package com.jamino.nimblerewynnded;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.world.GameMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.passive.StriderEntity;
import org.lwjgl.glfw.GLFW;

public class NimbleRewynnded implements ClientModInitializer {
    private static NimbleRewynnded instance;
    private static KeyBinding togglePerspectiveKey;
    private static KeyBinding frontViewKey;
    private Perspective lastPerspective = Perspective.FIRST_PERSON;
    private Perspective preRidingPerspective = Perspective.FIRST_PERSON;
    private boolean wasInSpectator = false;
    private boolean wasRiding = false;

    @Override
    public void onInitializeClient() {
        instance = this;
        togglePerspectiveKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nimblerewynnded.toggleperspective",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F5,
                "category.nimblerewynnded.general"
        ));

        frontViewKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nimblerewynnded.frontview",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.nimblerewynnded.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    public static NimbleRewynnded getInstance() {
        return instance;
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        handlePerspectiveToggle(client);
        handleFrontViewToggle(client);
        handleCutscenePerspective(client);
        handleRidingPerspective(client);
    }

    private void handlePerspectiveToggle(MinecraftClient client) {
        if (togglePerspectiveKey.wasPressed()) {
            Perspective currentPerspective = client.options.getPerspective();
            if (currentPerspective == Perspective.FIRST_PERSON) {
                client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            } else {
                client.options.setPerspective(Perspective.FIRST_PERSON);
            }
            lastPerspective = client.options.getPerspective();
            if (client.player.hasVehicle()) {
                preRidingPerspective = lastPerspective;
            }
        }
    }

    private void handleFrontViewToggle(MinecraftClient client) {
        if (frontViewKey.isPressed()) {
            if (client.options.getPerspective() != Perspective.THIRD_PERSON_FRONT) {
                lastPerspective = client.options.getPerspective();
                client.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
            }
        } else if (client.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            client.options.setPerspective(lastPerspective);
        }
    }

    private void handleCutscenePerspective(MinecraftClient client) {
        boolean isInSpectator = client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR;

        // Wynncraft-specific cutscene handling
        // This method detects when the player enters spectator mode, which is used for cutscenes in Wynncraft
        if (isInSpectator && !wasInSpectator) {
            lastPerspective = client.options.getPerspective();
            client.options.setPerspective(Perspective.FIRST_PERSON);
        } else if (!isInSpectator && wasInSpectator) {
            client.options.setPerspective(lastPerspective);
        }

        wasInSpectator = isInSpectator;
    }

    private void handleRidingPerspective(MinecraftClient client) {
        boolean isRiding = client.player.hasVehicle();

        if (isRiding != wasRiding) {
            if (isRiding) {
                Entity vehicle = client.player.getVehicle();
                if (!wasInSpectator && shouldChangePerspective(vehicle)) {
                    if (client.options.getPerspective() == Perspective.FIRST_PERSON ||
                            client.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
                        preRidingPerspective = client.options.getPerspective();
                        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                    } else {
                        preRidingPerspective = client.options.getPerspective();
                    }
                }
            } else {
                if (!wasInSpectator) {
                    client.options.setPerspective(preRidingPerspective);
                }
            }
            wasRiding = isRiding;
        }
    }
    //Add new entity classes here if wynncraft adds custom mounts
    private boolean shouldChangePerspective(Entity vehicle) {
        return vehicle instanceof AbstractMinecartEntity
                || vehicle instanceof HorseEntity
                || vehicle instanceof BoatEntity
                || vehicle instanceof StriderEntity
                || vehicle.getClass().getSimpleName().equals("class_1506");
    }

    public void mountEvent(Entity entity, boolean mount) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (entity != client.player) {
            return;
        }

        Entity vehicle = client.player.getVehicle();

        if (mount) {
            if (vehicle != null && shouldChangePerspective(vehicle)) {
                if (client.options.getPerspective() == Perspective.FIRST_PERSON ||
                        client.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
                    preRidingPerspective = client.options.getPerspective();
                    client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                } else {
                    preRidingPerspective = client.options.getPerspective();
                }
                wasRiding = true;
            }
        } else {
            if (wasRiding) {
                client.options.setPerspective(preRidingPerspective);
                wasRiding = false;
            }
        }
    }
}
