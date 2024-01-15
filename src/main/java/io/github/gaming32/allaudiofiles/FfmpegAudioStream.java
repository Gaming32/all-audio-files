package io.github.gaming32.allaudiofiles;

import com.github.manevolent.ffmpeg4j.AudioFrame;
import com.github.manevolent.ffmpeg4j.FFmpegException;
import com.github.manevolent.ffmpeg4j.FFmpegIO;
import com.github.manevolent.ffmpeg4j.FFmpegInput;
import com.github.manevolent.ffmpeg4j.MediaType;
import com.github.manevolent.ffmpeg4j.source.AudioSourceSubstream;
import com.github.manevolent.ffmpeg4j.stream.source.FFmpegSourceStream;
import com.github.manevolent.ffmpeg4j.stream.source.SourceStream;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.audio.OggAudioStream;
import net.minecraft.util.Mth;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVProbeData;
import org.bytedeco.javacpp.BytePointer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;

import static org.bytedeco.ffmpeg.global.avformat.AVPROBE_PADDING_SIZE;
import static org.bytedeco.ffmpeg.global.avformat.av_probe_input_format;

public class FfmpegAudioStream extends OggAudioStream {
    private static final int PROBE_SIZE = 16384;

    private FFmpegSourceStream stream;
    private boolean eof;
    private AudioSourceSubstream substream;
    private AudioFormat audioFormat;

    public FfmpegAudioStream(InputStream input) throws IOException {
        super(input);
        throw new UnsupportedOperationException("Use FfmpegAudioStream.create()");
    }

    public static FfmpegAudioStream create(InputStream input, String path) throws FFmpegException, IOException {
        final FfmpegAudioStream result;
        try {
            result = (FfmpegAudioStream)UnsafeUtil.UNSAFE.allocateInstance(FfmpegAudioStream.class);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        }

        if (!input.markSupported()) {
            input = new BufferedInputStream(input);
        }
        input.mark(PROBE_SIZE);
        final byte[] probeBuf = input.readNBytes(PROBE_SIZE);
        input.reset();

        final AVInputFormat ffmpegFormat;
        try (
            AVProbeData probeData = new AVProbeData();
            BytePointer bufPointer = new BytePointer(probeBuf.length + AVPROBE_PADDING_SIZE);
            BytePointer filenamePointer = new BytePointer(path)
        ) {
            bufPointer.put(probeBuf);
            probeData.buf(bufPointer);
            probeData.buf_size(probeBuf.length);
            probeData.filename(filenamePointer);

            ffmpegFormat = av_probe_input_format(probeData, 1);
        }

        final FFmpegInput ffmpegInput = FFmpegIO.openInputStream(input);
        result.stream = ffmpegInput.open(ffmpegFormat);

        return result;
    }

    private AudioSourceSubstream nextSubstream() throws IOException {
        if (eof) {
            return null;
        }
        if (substream != null) {
            return substream;
        }
        SourceStream.Packet packet;
        while (true) {
            try {
                packet = stream.readPacket();
            } catch (EOFException e) {
                eof = true;
                substream = null;
                return null;
            }
            if (packet != null && packet.getSourceStream().getMediaType() == MediaType.AUDIO) {
                return substream = (AudioSourceSubstream)packet.getSourceStream();
            }
        }
    }

    @NotNull
    @Override
    public AudioFormat getFormat() {
        try {
            return getFormat0();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @NotNull
    private AudioFormat getFormat0() throws IOException {
        if (audioFormat == null) {
            final AudioSourceSubstream substream = nextSubstream();
            if (substream == null) {
                throw new IllegalStateException("Cannot get format of empty audio stream");
            }
            final var format = substream.getFormat();
            audioFormat = new AudioFormat(
                format.getSampleRate(), 16, format.getChannels(), true, false
            );
        }
        return audioFormat;
    }

    @NotNull
    @Override
    @SuppressWarnings("StatementWithEmptyBody")
    public ByteBuffer read(int size) throws IOException {
        getFormat0();
        final OutputConcat output = new OutputConcat(size + 8192);
        while (readFrame(output) && output.byteCount < size);
        return output.get();
    }

    @NotNull
    @Override
    @SuppressWarnings("StatementWithEmptyBody")
    public ByteBuffer readAll() throws IOException {
        getFormat0();
        final OutputConcat output = new OutputConcat(16384);
        while (readFrame(output));
        return output.get();
    }

    private boolean readFrame(OutputConcat output) throws IOException {
        while (true) {
            AudioSourceSubstream substream = nextSubstream();
            if (substream == null) {
                return false;
            }

            AudioFrame nextFrame;
            try {
                nextFrame = substream.next();
            } catch (EOFException e) {
                return false;
            }

            while (nextFrame == null) {
                this.substream = null;
                substream = nextSubstream();
                if (substream == null) {
                    return false;
                }

                try {
                    nextFrame = substream.next();
                } catch (EOFException e) {
                    return false;
                }
            }

            final float[] samples = nextFrame.getSamples();
            if (samples.length == 0) continue;
            for (final float sample : samples) {
                output.put(sample);
            }
            return true;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            stream.close();
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class OutputConcat {
        private final List<ByteBuffer> buffers = Lists.newArrayList();
        private final int bufferSize;
        int byteCount;
        private ByteBuffer currentBuffer;

        public OutputConcat(int size) {
            this.bufferSize = size + 1 & -2;
            this.createNewBuffer();
        }

        /**
         * Creates a new buffer and sets it as the current buffer.
         */
        private void createNewBuffer() {
            this.currentBuffer = BufferUtils.createByteBuffer(this.bufferSize);
        }

        /**
         * Adds a sample to the current buffer. If the buffer is full, the buffer is added to the list of buffers and a new buffer is created.
         *
         * @param sample the audio sample to add to the buffer
         */
        public void put(float sample) {
            if (this.currentBuffer.remaining() == 0) {
                this.currentBuffer.flip();
                this.buffers.add(this.currentBuffer);
                this.createNewBuffer();
            }

            int i = Mth.clamp((int)(sample * 32767.5F - 0.5F), -32768, 32767);
            this.currentBuffer.putShort((short)i);
            this.byteCount += 2;
        }

        /**
         * {@return a single byte buffer containing all of the data from the list of buffers}
         */
        public ByteBuffer get() {
            this.currentBuffer.flip();
            if (this.buffers.isEmpty()) {
                return this.currentBuffer;
            } else {
                ByteBuffer byteBuffer = BufferUtils.createByteBuffer(this.byteCount);
                this.buffers.forEach(byteBuffer::put);
                byteBuffer.put(this.currentBuffer);
                byteBuffer.flip();
                return byteBuffer;
            }
        }
    }
}
