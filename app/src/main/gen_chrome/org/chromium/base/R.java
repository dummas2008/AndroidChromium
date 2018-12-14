/* AUTO-GENERATED FILE.  DO NOT MODIFY. */

package org.chromium.base;

public final class R {
    private static boolean sResourcesDidLoad;
    public static final class string {
        public static int product_version = 0x7f020000;
    }
    public static void onResourcesLoaded(int packageId) {
        assert !sResourcesDidLoad;
        sResourcesDidLoad = true;
        int packageIdTransform = (packageId ^ 0x7f) << 24;
        onResourcesLoadedString(packageIdTransform);
    }
    private static void onResourcesLoadedString (
            int packageIdTransform) {
        string.product_version ^= packageIdTransform;
    }
}