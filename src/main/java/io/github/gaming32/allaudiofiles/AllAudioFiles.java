package io.github.gaming32.allaudiofiles;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.FileToIdConverter;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class AllAudioFiles implements ClientModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final FileToIdConverter ALTERNATE_SOUND_LISTER = new FileToIdConverter("sounds", "");

    @Override
    public void onInitializeClient() {
        JavacppWrapper.init();

        try (Stream<AVInputFormat> stream = iterateDemuxers()) {
            LOGGER.info("Supported audio formats: {}", stream
                .map(AVInputFormat::name)
                .filter(Objects::nonNull)
                .map(BytePointer::getString)
                .collect(ImmutableSet.toImmutableSet())
            );
        }
    }

    private static <T extends Pointer> Stream<T> iterate(Function<Pointer, T> iterateFunction) {
        final Pointer opaque = new Pointer();
        return Stream.generate(() -> iterateFunction.apply(opaque))
            .takeWhile(Objects::nonNull)
            .onClose(opaque::close);
    }

    private static Stream<AVOutputFormat> iterateMuxers() {
        return iterate(avformat::av_muxer_iterate);
    }

    private static Stream<AVInputFormat> iterateDemuxers() {
        return iterate(avformat::av_demuxer_iterate);
    }

    private static Stream<AVCodec> iterateCodecs() {
        return iterate(avcodec::av_codec_iterate);
    }
}
