package io.github.dokuendev.dokuenreader.plugin.core;

import io.github.dokuendev.dokuenreader.plugin.core.ConfigField;

/**
 * A declarative schema describing all user-configurable settings your plugin requires.
 */
parcelable PluginConfigSchema {
    /**
     * The list of settings fields to show to the user.
     */
    ConfigField[] fields;
}
