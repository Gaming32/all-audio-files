package io.github.gaming32.allaudiofiles.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import io.github.gaming32.allaudiofiles.AllAudioFiles;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations")
public class MixinSoundManager_Preparations {
    @ModifyReceiver(
        method = "listResources",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;"
        )
    )
    private FileToIdConverter useAlternateLister(FileToIdConverter instance, ResourceManager resourceManager) {
        return AllAudioFiles.ALTERNATE_SOUND_LISTER;
    }
}
