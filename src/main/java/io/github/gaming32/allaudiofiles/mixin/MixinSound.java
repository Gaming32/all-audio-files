package io.github.gaming32.allaudiofiles.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import io.github.gaming32.allaudiofiles.AllAudioFiles;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Sound.class)
public class MixinSound {
    @ModifyReceiver(
        method = "getPath",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/resources/FileToIdConverter;idToFile(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceLocation;"
        )
    )
    private FileToIdConverter getPathWithExtension(FileToIdConverter instance, ResourceLocation id) {
        if (id.getPath().lastIndexOf('/') < id.getPath().lastIndexOf('.')) {
            return AllAudioFiles.ALTERNATE_SOUND_LISTER;
        }
        return instance;
    }
}
