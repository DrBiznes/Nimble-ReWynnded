package com.jamino.nimblerewynnded;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Perspective API switcher that keeps Nimble's toggle: each press moves to the next
 * perspective but never lands on the front view, which is reserved for the hold key.
 * With only the built-in perspectives this is a plain first person / third person toggle.
 * <p>
 * Loaded by Perspective API through {@link java.util.ServiceLoader}.
 */
public class NimbleSwitcher implements PerspectiveSwitcherBehavior {
    public static final String ID = NimbleRewynnded.MOD_ID + ".toggle_switcher";
    private static final Logger LOGGER = LoggerFactory.getLogger(NimbleSwitcher.class);

    private static NimbleSwitcher instance;

    private List<String> candidates = List.of();
    private String selected = NimbleRewynnded.FIRST_PERSON;

    public NimbleSwitcher() {
        instance = this;
    }

    /** Whether the player is currently using this switcher in Perspective API. */
    public static boolean isSelected() {
        return instance != null
                && PerspectiveAPI.isEnabled()
                && PerspectiveAPI.getSwitcherManager().getSelectedSwitcher() == instance;
    }

    /**
     * Selects the next perspective.
     *
     * @param from the perspective to move on from, or null to use the current selection
     */
    public static void toggle(String from) {
        if (instance != null) {
            instance.cycle(from != null ? from : instance.selected);
        }
    }

    /**
     * Makes this the active switcher the first time the mod runs, so the toggle works
     * out of the box. Afterwards the player's choice in Perspective API's settings is kept.
     */
    public static void selectOnFirstRun() {
        if (instance == null) return;
        Path marker = FabricLoader.getInstance().getConfigDir().resolve(NimbleRewynnded.MOD_ID + ".switcher_selected");
        if (Files.exists(marker)) return;
        try {
            PerspectiveAPI.getSwitcherManager().setSelectedSwitcher(instance);
            Files.createFile(marker);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not select the Nimble switcher by default", e);
        }
    }

    private void cycle(String from) {
        int size = candidates.size();
        int start = candidates.indexOf(from);
        for (int i = 1; i <= size; i++) {
            String next = candidates.get((start + i) % size);
            Perspective perspective = PerspectiveAPI.getRegistry().get(next);
            if (perspective != null && perspective.isAvailable()) {
                selected = next;
                return;
            }
        }
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Component name() {
        return Component.translatable("switcher.nimblerewynnded.toggle.name");
    }

    @Override
    public Component description() {
        return Component.translatable("switcher.nimblerewynnded.toggle.description");
    }

    @Override
    public void onSwitchablePerspectivesUpdated(List<Perspective> switchablePerspectives) {
        candidates = switchablePerspectives.stream()
                .map(perspective -> perspective.info().id())
                .filter(id -> !id.equals(NimbleRewynnded.THIRD_PERSON_FRONT))
                .toList();
    }

    @Override
    public void onActivated(Perspective currentPerspective) {
        String id = currentPerspective.info().id();
        if (candidates.contains(id)) {
            selected = id;
        }
    }

    @Override
    public void onDeactivated() {
    }

    @Override
    public String getSelectedPerspectiveId() {
        return selected;
    }
}
