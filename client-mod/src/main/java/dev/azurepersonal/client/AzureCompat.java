package dev.azurepersonal.client;

import net.minecraft.util.Identifier;
import java.lang.reflect.Method;

public final class AzureCompat {
    private static final Method OF_METHOD;

    static {
        Method method = null;
        try {
            method = Identifier.class.getMethod("of", String.class, String.class);
        } catch (NoSuchMethodException ignored) {
        }
        OF_METHOD = method;
    }

    private AzureCompat() {}

    public static Identifier id(String namespace, String path) {
        ReflectiveOperationException factoryFailure = null;
        if (OF_METHOD != null) {
            try {
                return (Identifier) OF_METHOD.invoke(null, namespace, path);
            } catch (ReflectiveOperationException e) {
                factoryFailure = e;
            }
        }
        try {
            return Identifier.class.getConstructor(String.class, String.class).newInstance(namespace, path);
        } catch (ReflectiveOperationException e) {
            if (factoryFailure != null) {
                e.addSuppressed(factoryFailure);
            }
            throw new IllegalStateException("Unable to create Minecraft identifier", e);
        }
    }
}
