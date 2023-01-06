package org.gradle.android.workarounds.room.androidvariants

/**
 * Represents equivalent configurations for AGP versions that support either the old or new variant APIs.
 */
interface ConfigureVariants {
    /**
     * The equivalent configuration for the old variant API.  Closure will receive a {@link com.android.build.gradle.api.BaseVariant}
     * as the argument.
     */
    Closure<?> getOldVariantConfiguration();
    /**
     * The equivalient configuration for the new variant API.  Closure will receive a {@link com.android.build.api.variant.Variant}
     * as the argument.
     */
    Closure<?> getNewVariantConfiguration();
}
