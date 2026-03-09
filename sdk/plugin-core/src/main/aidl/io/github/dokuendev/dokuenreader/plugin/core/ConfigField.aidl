package io.github.dokuendev.dokuenreader.plugin.core;

/**
 * Represents a single user-configurable setting required by the plugin.
 * The Dokuen host app reads this to dynamically build a settings UI for the user.
 */
parcelable ConfigField {
    /**
     * The unique key used to identify this field in the config Bundle.
     *
     * Keys may not start with an underscore (`_`), as those are reserved for
     * `PluginHostConfigKeys`.
     */
    String key;
    
    /**
     * The user-facing label shown in the settings UI.
     */
    String displayName;
    
    /**
     * A longer description of what this setting does, shown as subtext in the UI.
     */
    String description;
    
    /**
     * The data type of the setting.
     * 
     * Use constants from ConfigFieldType:
     * - ConfigFieldType.STRING (0) = String input field
     * - ConfigFieldType.BOOLEAN (1) = Boolean checkbox/switch
     * - ConfigFieldType.INT (2) = Integer input field
     * - ConfigFieldType.ENUM (3) = Dropdown selection from enumValues
     */
    int type;
    
    /**
     * The default value presented to the user before they change it, serialized as a String.
     */
    @nullable String defaultValue;
    
    /**
     * Whether the user must provide a value for this field before initialization.
     */
    boolean isRequired;
    
    /**
     * For ENUM type fields, the list of acceptable string values.
     * The UI displays these as a dropdown menu.
     * Must be non-empty when type is ENUM, ignored for other types.
     */
    @nullable String[] enumValues;
}
