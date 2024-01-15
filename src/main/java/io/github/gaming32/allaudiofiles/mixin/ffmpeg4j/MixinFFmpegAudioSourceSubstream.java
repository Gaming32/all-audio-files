package io.github.gaming32.allaudiofiles.mixin.ffmpeg4j;

import com.github.manevolent.ffmpeg4j.source.FFmpegAudioSourceSubstream;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import static org.bytedeco.ffmpeg.global.avutil.av_get_default_channel_layout;

@Mixin(value = FFmpegAudioSourceSubstream.class, remap = false)
public class MixinFFmpegAudioSourceSubstream {
    @Shadow @Final private int outputChannels;

    // Funny bugfix with wav files. See https://stackoverflow.com/a/20049638/8840278.
    @ModifyExpressionValue(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/bytedeco/ffmpeg/avcodec/AVCodecParameters;channel_layout()J"
        )
    )
    private long ensurePresentChannelLayout(long original) {
        if (original == 0L) {
            return av_get_default_channel_layout(outputChannels);
        }
        return original;
    }
}
