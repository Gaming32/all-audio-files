package io.github.gaming32.allaudiofiles.mixin.javacpp;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.github.gaming32.allaudiofiles.CompoundEnumeration;
import io.github.gaming32.allaudiofiles.JavacppWrapper;
import org.bytedeco.javacpp.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

@Mixin(value = Loader.class, remap = false)
public class MixinLoader {
    @WrapOperation(
        method = "extractResource(Ljava/net/URL;Ljava/io/File;Ljava/lang/String;Ljava/lang/String;Z)Ljava/io/File;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/io/File;delete()Z",
            ordinal = 1
        )
    )
    private static boolean rememberDeleteResult(
        File instance, Operation<Boolean> original,
        @Share("deleteHack") LocalBooleanRef deleteHack
    ) {
        deleteHack.set(!original.call(instance) && instance.isFile());
        return deleteHack.get();
    }

    @Inject(
        method = "extractResource(Ljava/net/URL;Ljava/io/File;Ljava/lang/String;Ljava/lang/String;Z)Ljava/io/File;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/io/File;delete()Z",
            ordinal = 1,
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private static void useDeleteResult(
        URL resourceURL,
        File directoryOrFile,
        String prefix,
        String suffix,
        boolean cacheDirectory,
        CallbackInfoReturnable<File> cir,
        @Local(ordinal = 1) File file,
        @Share("deleteHack") LocalBooleanRef deleteHack
    ) {
        if (deleteHack.get()) {
            cir.setReturnValue(file);
        }
    }

    @WrapOperation(
        method = "loadProperties(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Properties;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Class;getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;"
        )
    )
    private static InputStream customResources(Class<?> instance, String name, Operation<InputStream> original) {
        final InputStream result = JavacppWrapper.getResourceAsStream(name, instance);
        return result != null ? result : original.call(instance, name);
    }

    @WrapOperation(
        method = "getVersion(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/String;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/ClassLoader;getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;"
        )
    )
    private static InputStream customResources(ClassLoader instance, String name, Operation<InputStream> original) {
        final InputStream result = JavacppWrapper.getResourceAsStream(name, null);
        return result != null ? result : original.call(instance, name);
    }

    @WrapOperation(
        method = "findResources(Ljava/lang/Class;Ljava/lang/String;I)[Ljava/net/URL;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Class;getResource(Ljava/lang/String;)Ljava/net/URL;"
        )
    )
    private static URL customResources1(Class<?> instance, String name, Operation<URL> original) {
        final URL result = JavacppWrapper.getResource(name, instance);
        return result != null ? result : original.call(instance, name);
    }

    @WrapOperation(
        method = "findResources(Ljava/lang/Class;Ljava/lang/String;I)[Ljava/net/URL;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/ClassLoader;getResources(Ljava/lang/String;)Ljava/util/Enumeration;"
        )
    )
    private static Enumeration<URL> customResources1(ClassLoader instance, String name, Operation<Enumeration<URL>> original) {
        return new CompoundEnumeration<>(JavacppWrapper.getResources(name, null), original.call(instance, name));
    }
}
