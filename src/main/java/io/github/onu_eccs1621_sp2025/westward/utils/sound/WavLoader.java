package io.github.onu_eccs1621_sp2025.westward.utils.sound;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.openal.AL10;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Loads WAV files into OpenAL
 * @author Dylan Catte
 * @since 1.0.0 Beta 5
 * @version 1.0
 */
public class WavLoader {

    private static boolean isBigEndian() {
        return ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    }

    private static int convertToInt(byte[] buffer, int len) {
        int a = 0;
        if (!isBigEndian()) {
            for (int i = 0; i < len; i++) {
                a |= (buffer[i] & 0xFF) << (8 * i);
            }
        } else {
            for (int i = 0; i < len; i++) {
                a |= (buffer[i] & 0xFF) << (8 * (len - 1 - i));
            }
        }
        return a;
    }

    private static ByteBuffer loadWAV(String filePath, int[] outChannels, int[] outSampleRate, int[] outBps) throws IOException {
        File file = new File(filePath);
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer4 = new byte[4];
            byte[] buffer2 = new byte[2];

            // RIFF header
            in.read(buffer4);
            if (!new String(buffer4).equals("RIFF")) throw new IOException("Not a WAV file");

            in.read(buffer4); // chunk size
            in.read(buffer4); // WAVE
            in.read(buffer4); // fmt
            in.read(buffer4); // fmt chunk size
            in.read(buffer2); // audio format (1 = PCM)
            in.read(buffer2); // channels
            outChannels[0] = convertToInt(buffer2, 2);

            in.read(buffer4); // sample rate
            outSampleRate[0] = convertToInt(buffer4, 4);

            in.read(buffer4); // byte rate
            in.read(buffer2); // block align
            in.read(buffer2); // bits per sample
            outBps[0] = convertToInt(buffer2, 2);

            // data chunk
            in.read(buffer4); // "data"
            in.read(buffer4); // data size
            int dataSize = convertToInt(buffer4, 4);

            byte[] audioData = new byte[dataSize];
            in.read(audioData);

            // Convert to ByteBuffer for OpenAL
            ByteBuffer data = MemoryUtil.memAlloc(dataSize);
            data.put(audioData);
            data.flip();

            return data;
        }
    }

    /**
     * Creates a Sound instance from a WAV file path
     * @param path Path to WAV file
     * @return Sound instance
     */
    public static SoundEngine.Sound createSound(String path) throws IOException {
        int[] channels = new int[1];
        int[] sampleRate = new int[1];
        int[] bps = new int[1];

        ByteBuffer data = loadWAV(path, channels, sampleRate, bps);

        int format;
        if (channels[0] == 1) {
            format = (bps[0] == 8) ? AL10.AL_FORMAT_MONO8 : AL10.AL_FORMAT_MONO16;
        } else {
            format = (bps[0] == 8) ? AL10.AL_FORMAT_STEREO8 : AL10.AL_FORMAT_STEREO16;
        }

        SoundEngine.Sound sound = new SoundEngine.Sound();
        sound.buffer = AL10.alGenBuffers();
        AL10.alBufferData(sound.buffer, format, data, sampleRate[0]);
        MemoryUtil.memFree(data);

        sound.source = AL10.alGenSources();
        AL10.alSourcei(sound.source, AL10.AL_BUFFER, sound.buffer);

        return sound;
    }
}
