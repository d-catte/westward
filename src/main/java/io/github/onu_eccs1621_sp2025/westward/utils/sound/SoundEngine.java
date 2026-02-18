package io.github.onu_eccs1621_sp2025.westward.utils.sound;

import io.github.onu_eccs1621_sp2025.westward.utils.Config;
import io.github.onu_eccs1621_sp2025.westward.utils.DebugLogger;
import io.github.onu_eccs1621_sp2025.westward.utils.registry.Registry;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Manages the audio playback for the game using Java Clip API
 * @author Dylan Catte
 * @since 1.0.0 Beta 1
 * @version 3.0
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

    // For music
    private static String lastMusicId;
    private static Clip lastMusic;

    // For SFX
    private static String lastSfxId;
    private static Clip lastSfx;

    // Force skip if sound system failed
    private static boolean skipSounds = false;

    /**
     * Initialize SoundEngine class
     */
    public static void init() {}

    /**
     * Clean up sound resources. Calls at shutdown.
     */
    public static void destroy() {
        if (!skipSounds) {
            if (lastMusic != null && lastMusic.isOpen()) {
                lastMusic.close();
            }

            if (lastSfx != null && lastSfx.isOpen()) {
                lastSfx.close();
            }
        }
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

    private static Clip load(Path path)
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

    private static Clip loadWav(Path path)
            throws IOException, UnsupportedAudioFileException, LineUnavailableException {

        try (AudioInputStream in = AudioSystem.getAudioInputStream(path.toFile())) {
            Clip clip = AudioSystem.getClip();
            clip.open(in);
            return clip;
        }
    }

    private static Clip loadOgg(Path path)
            throws IOException, UnsupportedAudioFileException, LineUnavailableException {

        try (AudioInputStream in = AudioSystem.getAudioInputStream(path.toFile())) {
             AudioFormat baseFormat = in.getFormat();

             AudioFormat decodedFormat = new AudioFormat(
                     AudioFormat.Encoding.PCM_SIGNED,
                     baseFormat.getSampleRate(),
                     16,
                     baseFormat.getChannels(),
                     baseFormat.getChannels() * 2,
                     baseFormat.getSampleRate(),
                     false
                );

             try (AudioInputStream din =
                          AudioSystem.getAudioInputStream(decodedFormat, in)) {

                 ByteArrayOutputStream out = new ByteArrayOutputStream();
                 byte[] buffer = new byte[4096];
                 int bytesRead;

                 while ((bytesRead = din.read(buffer)) != -1) {
                     out.write(buffer, 0, bytesRead);
                 }

                 byte[] audioBytes = out.toByteArray();

                 if (audioBytes.length == 0) {
                     throw new IOException("Decoded OGG produced 0 PCM bytes: " + path);
                 }

                 Clip clip = AudioSystem.getClip();
                 clip.open(decodedFormat, audioBytes, 0, audioBytes.length);
                 return clip;
             }
        }
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
                lastMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }
            updateMusicVolume();
            playMusic();
        } else {
            if (shouldLoop) {
                lastSfx.loop(Clip.LOOP_CONTINUOUSLY);
            }
            updateSfxVolume();
            playSFX();
        }
    }

    private static void playMusic() {
        if (skipSounds) {
            return;
        }
        lastMusic.setFramePosition(0);
        lastMusic.start();
    }

    /**
     * Stops the currently playing music
     */
    public static void stopMusic() {
        lastMusic.stop();
    }

    private static void playSFX() {
        if (skipSounds) {
            return;
        }
        lastSfx.setFramePosition(0);
        lastSfx.start();
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

    private static void setClipVolume(Clip clip, float volume) {
        if (volume <= 0.0F) {
            volume = 0.0001F;
        }

        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl =
                    (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(20.0F * (float) Math.log10(volume));
        }
    }


    /**
     * Invalidates cached id data
     */
    public static void invalidateCaches() {
        lastMusicId = null;
        lastSfxId   = null;
    }
}