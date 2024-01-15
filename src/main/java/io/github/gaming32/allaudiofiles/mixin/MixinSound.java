package io.github.gaming32.allaudiofiles.mixin;

import io.github.gaming32.allaudiofiles.AllAudioFiles;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sound.class)
public class MixinSound {
    @Shadow @Final private ResourceLocation location;

    @Inject(method = "getPath", at = @At("HEAD"), cancellable = true)
    private void getPathWithExtension(CallbackInfoReturnable<ResourceLocation> cir) {
        final String pathText = location.getPath();
        if (pathText.lastIndexOf('/') < pathText.lastIndexOf('.')) {
            cir.setReturnValue(AllAudioFiles.ALTERNATE_SOUND_LISTER.idToFile(location));
        }
    }
}
