package ee.gaile.estoniantts.service;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WavVolumeTest {

    private static final int SAMPLE_RATE = 22050;

    @Test
    void pcm16IsScaledLikeTheFormerPythonScript() throws Exception {
        byte[] wav = pcm16Wav(new short[]{1000, -1000, 3, -3, Short.MAX_VALUE, Short.MIN_VALUE});

        byte[] result = WavVolume.scale(wav, 0.5);

        // int(s * 0.5) in Python truncates towards zero
        assertThat(samples(result)).containsExactly(500, -500, 1, -1, 16383, -16384);
    }

    @Test
    void loudSamplesAreClipped() throws Exception {
        byte[] result = WavVolume.scale(pcm16Wav(new short[]{20000, -20000}), 2.0);

        assertThat(samples(result)).containsExactly(Short.MAX_VALUE, Short.MIN_VALUE);
    }

    @Test
    void float32FromWorkerV31IsConvertedToPcm16() throws Exception {
        byte[] wav = float32Wav(new float[]{0.5f, -0.5f, 0f});

        byte[] result = WavVolume.scale(wav, 0.5);

        AudioFormat format = AudioSystem.getAudioFileFormat(new ByteArrayInputStream(result)).getFormat();
        assertThat(format.getEncoding()).isEqualTo(AudioFormat.Encoding.PCM_SIGNED);
        assertThat(format.getSampleSizeInBits()).isEqualTo(16);
        assertThat(format.getSampleRate()).isEqualTo(SAMPLE_RATE);

        short[] samples = samples(result);
        assertThat(samples).hasSize(3);
        assertThat((int) samples[0]).isCloseTo(8192, within(1));
        assertThat((int) samples[1]).isCloseTo(-8192, within(1));
        assertThat(samples[2]).isZero();
    }

    private static short[] samples(byte[] wav) throws Exception {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            ByteBuffer data = ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
            short[] samples = new short[data.remaining() / 2];
            data.asShortBuffer().get(samples);
            return samples;
        }
    }

    /**
     * The same layout as scipy.io.wavfile.write produces for int16 data (worker v3.0).
     */
    private static byte[] pcm16Wav(short[] samples) {
        ByteBuffer data = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : samples) {
            data.putShort(s);
        }
        return wav(1, 16, new byte[0], data.array(), samples.length);
    }

    /**
     * The same layout as scipy.io.wavfile.write produces for float32 data (worker v3.1):
     * IEEE float format tag, cbSize in the fmt chunk and a fact chunk.
     */
    private static byte[] float32Wav(float[] samples) {
        ByteBuffer data = ByteBuffer.allocate(samples.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float s : samples) {
            data.putFloat(s);
        }
        return wav(3, 32, new byte[]{0, 0}, data.array(), samples.length);
    }

    private static byte[] wav(int formatTag, int bitsPerSample, byte[] fmtExtension, byte[] data, int frames) {
        boolean pcm = formatTag == 1;
        int fmtSize = 16 + fmtExtension.length;
        int factChunkSize = pcm ? 0 : 12;
        int blockAlign = bitsPerSample / 8;

        ByteBuffer wav = ByteBuffer.allocate(12 + 8 + fmtSize + factChunkSize + 8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes()).putInt(wav.capacity() - 8).put("WAVE".getBytes());
        wav.put("fmt ".getBytes()).putInt(fmtSize)
                .putShort((short) formatTag).putShort((short) 1)
                .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * blockAlign)
                .putShort((short) blockAlign).putShort((short) bitsPerSample)
                .put(fmtExtension);
        if (!pcm) {
            wav.put("fact".getBytes()).putInt(4).putInt(frames);
        }
        wav.put("data".getBytes()).putInt(data.length).put(data);
        return wav.array();
    }
}
