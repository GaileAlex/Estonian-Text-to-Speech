package ee.gaile.estoniantts.service;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Volume adjustment of the WAV files produced by the TTS worker.
 */
final class WavVolume {

    private WavVolume() {
    }

    /**
     * Multiplies every sample by the factor. The input may be 16-bit PCM (worker v3.0) or 32-bit float (worker v3.1),
     * the result is always 16-bit PCM.
     */
    static byte[] scale(byte[] wav, double factor) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            AudioFormat sourceFormat = source.getFormat();
            AudioFormat pcm16 = new AudioFormat(sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), true, false);

            byte[] pcm;
            try (AudioInputStream converted = AudioSystem.getAudioInputStream(pcm16, source)) {
                pcm = converted.readAllBytes();
            }

            ShortBuffer samples = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            for (int i = 0; i < samples.limit(); i++) {
                int scaled = (int) (samples.get(i) * factor);
                samples.put(i, (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaled)));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AudioInputStream result = new AudioInputStream(new ByteArrayInputStream(pcm), pcm16, pcm.length / pcm16.getFrameSize());
            AudioSystem.write(result, AudioFileFormat.Type.WAVE, out);
            return out.toByteArray();
        }
    }
}
