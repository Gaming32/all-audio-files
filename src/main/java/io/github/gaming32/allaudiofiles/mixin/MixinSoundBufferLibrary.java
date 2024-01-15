package io.github.gaming32.allaudiofiles.mixin;

import com.github.manevolent.ffmpeg4j.FFmpegException;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.OggAudioStream;
import io.github.gaming32.allaudiofiles.FfmpegAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;

@Mixin(SoundBufferLibrary.class)
public class MixinSoundBufferLibrary {
    @Redirect(
        method = {"lambda$getCompleteBuffer$0", "lambda$getStream$2"},
        at = @At(
            value = "NEW",
            target = "(Ljava/io/InputStream;)Lcom/mojang/blaze3d/audio/OggAudioStream;"
        )
    )
    private OggAudioStream useFfmpeg(InputStream input, @Local ResourceLocation location) throws IOException {
        try {
            return FfmpegAudioStream.create(input, location.getPath());
        } catch (FFmpegException e) {
            throw new IOException(e);
        }
    }

    @WrapOperation(
        method = "lambda$getStream$2",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/client/sounds/LoopingAudioStream$AudioStreamProvider;Ljava/io/InputStream;)Lnet/minecraft/client/sounds/LoopingAudioStream;"
        )
    )
    private LoopingAudioStream useFfmpeg(
        LoopingAudioStream.AudioStreamProvider provider, InputStream inputStream,
        Operation<LoopingAudioStream> original,
        @Local ResourceLocation location
    ) {
        final String path = location.getPath();
        return original.call((LoopingAudioStream.AudioStreamProvider)input -> {
            try {
                return FfmpegAudioStream.create(input, path);
            } catch (FFmpegException e) {
                throw new IOException(e);
            }
        }, inputStream);
    }
}
