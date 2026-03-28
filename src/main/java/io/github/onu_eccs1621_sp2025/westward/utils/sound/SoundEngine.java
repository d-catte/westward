package io.github.onu_eccs1621_sp2025.westward.utils.sound;

import io.github.onu_eccs1621_sp2025.westward.utils.Config;
import io.github.onu_eccs1621_sp2025.westward.utils.DebugLogger;
import io.github.onu_eccs1621_sp2025.westward.utils.registry.Registry;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;

/**
 * Manages the audio playback for the game using OpenAL
 * @author Dylan Catte
 * @since 1.0.0 Beta 1
 * @version 4.0
 */
public final class SoundEngine {
    /**
     * Music Volume from 0-100
     */
    public static final int[] MUSIC_VOLUME = { Config.getConfig().getMusicVolume() };
    /**
     * Sound Effect Volume from 0-100
     */
    public static final int[] SFX_VOLUME   = { Config.getConfig().getSfxVolume() };

    // OpenAL
    private static long device;
    private static long context;

    // For music
    private static String lastMusicId;
    private static Sound lastMusic;

    // For SFX
    private static String lastSfxId;
    private static Sound lastSfx;

    // Force skip if sound system failed
    private static boolean skipSounds = false;

    // OpenAL representation to Java's Clip
    public static class Sound {
        int buffer;
        int source;
        boolean loop;

        /**
         * Starts playing a sound
         */
        public void play() {
            AL10.alSourceStop(this.source);
            AL10.alSourcei(this.source, AL10.AL_LOOPING, loop ? AL10.AL_TRUE : AL10.AL_FALSE);
            AL10.alSourceRewind(this.source);
            AL10.alSourcePlay(this.source);
        }

        /**
         * Sets the volume of a sound (0-100)
         * @param volume Volume of the sound
         */
        public void setVolume(float volume) {
            AL10.alSourcef(this.source, AL10.AL_GAIN, volume);
        }

        /**
         * Analogous to Clip's close method
         */
        public void close() {
            AL10.alDeleteSources(this.source);
            AL10.alDeleteBuffers(this.buffer);
        }

        /**
         * Whether the audio should loop or not
         * @param looping Loop audio
         */
        public void setLooping(boolean looping) {
            this.loop = looping;
        }

        /**
         * Pauses the currently playing audio
         */
        public void pause() {
            AL10.alSourcePause(this.source);
        }
    }

    /**
     * Initialize SoundEngine class
     */
    public static void init() {
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        if (device == 0) throw new IllegalStateException("Failed to open OpenAL device");

        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));
    }

    /**
     * Clean up sound resources. Calls at shutdown.
     */
    public static void destroy() {
        if (lastMusic != null) {
            lastMusic.close();
        }

        if (lastSfx != null) {
            lastSfx.close();
        }

        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
    }

    /**
     * Loads a song into the SoundEngine
     * @param songId The name/id of the song
     * @param loop If the song should loop
     */
    public static void loadSong(final String songId, final boolean loop) {
        if (skipSounds) {
            return;
        }
        if (songId.equals(lastMusicId)) {
            // restart
            playMusic();
        } else {
            if (lastMusic != null) {
                lastMusic.close();
            }
            lastMusicId = songId;
            Path path = (Path) Registry.getAsset(Registry.AssetType.AUDIO, songId);
            try {
                lastMusic = load(path);
                process(loop, true);
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException exception) {
                DebugLogger.warn("Failed to load song: ", exception.getMessage());
                skipSounds  = true;
            }
        }
    }

    private static Sound load(Path path)
            throws IOException, UnsupportedAudioFileException, LineUnavailableException {

        String name = path.toString().toLowerCase();

        if (name.endsWith(".wav")) {
            return loadWav(path);
        } else if (name.endsWith(".ogg")) {
            return loadOgg(path);
        } else {
            throw new UnsupportedAudioFileException("Unsupported format: " + name);
        }
    }

    private static Sound loadWav(Path path) {
        try {
            return WavLoader.createSound(String.valueOf(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Sound loadOgg(Path path) {
        Sound sound = new Sound();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);

            ShortBuffer rawAudio = STBVorbis.stb_vorbis_decode_filename(
                    path.toString(),
                    channels,
                    sampleRate
            );

            if (rawAudio == null) {
                throw new RuntimeException("Failed to decode OGG: " + path);
            }

            int format = channels.get(0) == 1
                    ? AL10.AL_FORMAT_MONO16
                    : AL10.AL_FORMAT_STEREO16;

            sound.buffer = AL10.alGenBuffers();
            AL10.alBufferData(sound.buffer, format, rawAudio, sampleRate.get(0));

            sound.source = AL10.alGenSources();
            AL10.alSourcei(sound.source, AL10.AL_BUFFER, sound.buffer);
        }

        return sound;
    }


    /**
     * Loads a sound effect into the SoundEngine
     * @param sfxId The name/id of the sfx
     */
    public static void loadSFX(final String sfxId) {
        if (skipSounds) {
            return;
        }
        if (sfxId.equals(lastSfxId)) {
            playSFX();
        } else {
            if (lastSfx != null) {
                lastSfx.close();
            }
            lastSfxId = sfxId;
            Path path = (Path) Registry.getAsset(Registry.AssetType.SFX, sfxId);
            try {
                lastSfx = load(path);
                process(false, false);
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException exception) {
                DebugLogger.warn("Failed to load SFX: ", exception.getMessage());
                skipSounds = true;
            }
        }
    }

    /**
     * Loads a random song into the SoundEngine
     * @param loop If the song should loop
     */
    public static void loadRandomSong(final boolean loop) {
        if (!skipSounds) {
            final Path song = (Path) Registry.randomAsset(Registry.AssetType.AUDIO);
            try {
                lastMusic = load(song);
                process(loop, true);
            } catch (IOException | LineUnavailableException | UnsupportedAudioFileException ignored) {
            }
        }
    }

    private static void process(boolean shouldLoop, boolean music) {
        if (music) {
            // Reset music
            stopMusic();
            if (shouldLoop) {
                lastMusic.setLooping(true);
            }
            updateMusicVolume();
            playMusic();
        } else {
            if (shouldLoop) {
                lastSfx.setLooping(true);
            }
            updateSfxVolume();
            playSFX();
        }
    }

    private static void playMusic() {
        if (skipSounds) {
            return;
        }
        lastMusic.play();
    }

    /**
     * Stops the currently playing music
     */
    public static void stopMusic() {
        lastMusic.pause();
    }

    private static void playSFX() {
        if (skipSounds) {
            return;
        }
        lastSfx.play();
    }

    /**
     * Refreshes the music's volume
     */
    public static void updateMusicVolume() {
        setClipVolume(lastMusic, MUSIC_VOLUME[0] / 100.0F);
    }

    /**
     * Refreshes the sfx's volume
     */
    public static void updateSfxVolume() {
        setClipVolume(lastSfx, SFX_VOLUME[0] / 100.0F);
    }

    private static void setClipVolume(Sound clip, float volume) {
        if (volume <= 0.0F) {
            volume = 0.0001F;
        }

        clip.setVolume(volume);
    }

    /**
     * Invalidates cached id data
     */
    public static void invalidateCaches() {
        lastMusicId = null;
        lastSfxId   = null;
    }
}