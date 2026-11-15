package com.lowcode.svintusclient.client.module.setting;

import java.util.List;

public class ModeSetting extends Setting<String> {

    private final List<String> modes;

    public ModeSetting(String name, String... modes) {
        super(name, modes != null && modes.length > 0 ? modes[0] : "");
        this.modes = modes != null ? java.util.Arrays.stream(modes).distinct().toList() : java.util.List.of();
    }

    public List<String> getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return value.equals(mode);
    }

    public void cycle() {
        int idx = modes.indexOf(value);
        int next = (idx + 1) % modes.size();
        set(modes.get(next));
    }
}
