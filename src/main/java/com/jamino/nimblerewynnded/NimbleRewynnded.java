package com.jamino.nimblerewynnded;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.jamino.nimblerewynnded.perspective.FreeThirdPersonPerspective;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.monster.Strider;
import org.lwjgl.glfw.GLFW;

public class NimbleRewynnded implements ClientModInitializer {
    public static final String MOD_ID = "nimblerewynnded";

    // Perspective API's built-in perspectives
    public static final String FIRST_PERSON = "perspective_api.first_person";
    public static final String THIRD_PERSON_BACK = "perspective_api.third_person_back";
    public static final String THIRD_PERSON_FRONT = "perspective_api.third_person_front";

    // priority:perspective_api.override
    private static final int RIDING_PRIORITY = 1000;
    private static final int FREE_THIRD_PERSON_PRIORITY = 1500;
    private static final int FRONT_VIEW_PRIORITY = 2000;
    private static final int CUTSCENE_PRIORITY = 3000;

    private KeyMapping togglePerspectiveKey;
    private KeyMapping frontViewKey;
    private KeyMapping freeThirdPersonKey;

    // Updated on client ticks, read by the override suppliers on render frames
    private boolean ridingThirdPerson;
    private boolean frontViewHeld;
    private boolean freeThirdPerson;
    private boolean inCutscene;
    private boolean wasRiding;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("nimblerewynnded:keys"));
        togglePerspectiveKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.nimblerewynnded.toggleperspective",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F5,
                category
        ));

        frontViewKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.nimblerewynnded.frontview",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                category
        ));

        freeThirdPersonKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.nimblerewynnded.freethirdperson",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        // Each override temporarily replaces the perspective picked by the player's switcher.
        // When it returns null again, Perspective API falls back to whatever is below it,
        // so we never have to remember and restore the previous perspective ourselves.
        PerspectiveAPI.runWhenReady(MOD_ID, () -> {
            PerspectiveOverrideChain overrides = PerspectiveAPI.getOverrideChain();
            overrides.register(RIDING_PRIORITY, () -> ridingThirdPerson ? THIRD_PERSON_BACK : null);
            overrides.register(FREE_THIRD_PERSON_PRIORITY, () -> freeThirdPerson ? FreeThirdPersonPerspective.ID : null);
            overrides.register(FRONT_VIEW_PRIORITY, () -> frontViewHeld ? THIRD_PERSON_FRONT : null);
            overrides.register(CUTSCENE_PRIORITY, () -> inCutscene ? FIRST_PERSON : null);
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> NimbleSwitcher.selectOnFirstRun());
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            ridingThirdPerson = false;
            frontViewHeld = false;
            freeThirdPerson = false;
            inCutscene = false;
            wasRiding = false;
            return;
        }

        if (freeThirdPersonKey.consumeClick()) {
            freeThirdPerson = !freeThirdPerson;
        }
        handlePerspectiveToggle();
        frontViewHeld = frontViewKey.isDown();
        // Wynncraft-specific cutscene handling
        // Wynncraft puts the player in spectator mode for cutscenes
        inCutscene = client.gameMode.getPlayerMode() == GameType.SPECTATOR;
        handleRidingPerspective(client);
    }

    private void handlePerspectiveToggle() {
        if (togglePerspectiveKey.consumeClick() && NimbleSwitcher.isSelected()) {
            if (freeThirdPerson) {
                // Leave free third person and go back to the player's own perspective
                freeThirdPerson = false;
                return;
            }
            // While the riding override is showing third person, toggle away from that
            // and keep the player's choice for the rest of the ride and after dismounting
            NimbleSwitcher.toggle(ridingThirdPerson ? THIRD_PERSON_BACK : null);
            ridingThirdPerson = false;
        }
    }

    private void handleRidingPerspective(Minecraft client) {
        boolean isRiding = client.player.isPassenger();

        if (isRiding != wasRiding) {
            if (isRiding) {
                Entity vehicle = client.player.getVehicle();
                if (vehicle != null && !inCutscene && shouldChangePerspective(vehicle)) {
                    ridingThirdPerson = PerspectiveAPI.isCurrent(FIRST_PERSON)
                            || PerspectiveAPI.isCurrent(THIRD_PERSON_FRONT);
                }
            } else {
                ridingThirdPerson = false;
            }
            wasRiding = isRiding;
        }
    }

    //Add new entity classes here if wynncraft adds custom mounts
    private boolean shouldChangePerspective(Entity vehicle) {
        String className = vehicle.getClass().getName();
        // Fallback for custom Wynncraft entities identified by intermediary names
        if (className.contains("class_1501") || className.contains("class_1621")) {
            return true;
        }

        return vehicle instanceof AbstractMinecart
                || vehicle instanceof Horse
                || vehicle instanceof SkeletonHorse
                || vehicle instanceof ZombieHorse
                || vehicle instanceof Camel
                || vehicle instanceof Boat
                || vehicle instanceof Strider
                || vehicle instanceof ArmorStand;
    }
}
