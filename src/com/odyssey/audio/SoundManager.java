package com.odyssey.audio;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.libc.LibCStdlib.free;

public class SoundManager {

    private long device;
    private long context;

    private final List<Integer> buffers = new ArrayList<>();
    private final List<Integer> sources = new ArrayList<>();

    public void init() {
        device = alcOpenDevice((CharSequence) null);
        if (device == 0L) {
            throw new IllegalStateException("Failed to open the default OpenAL device.");
        }
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);

        context = alcCreateContext(device, (IntBuffer) null);
        alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);
    }

    public int loadSound(String filePath) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer channelsBuffer = stack.mallocInt(1);
            IntBuffer sampleRateBuffer = stack.mallocInt(1);

            ShortBuffer rawAudioBuffer = stb_vorbis_decode_filename(filePath, channelsBuffer, sampleRateBuffer);
            if (rawAudioBuffer == null) {
                System.err.println("Warning: Could not load sound: " + filePath + " - Audio will be disabled for this sound");
                return -1;
            }

            int channels = channelsBuffer.get();
            int sampleRate = sampleRateBuffer.get();
            
            int format = -1;
            if (channels == 1) {
                format = AL_FORMAT_MONO16;
            } else if (channels == 2) {
                format = AL_FORMAT_STEREO16;
            }

            int bufferPointer = alGenBuffers();
            alBufferData(bufferPointer, format, rawAudioBuffer, sampleRate);
            
            free(rawAudioBuffer);
            
            buffers.add(bufferPointer);
            return bufferPointer;
        } catch (Exception e) {
            System.err.println("Warning: Exception loading sound " + filePath + ": " + e.getMessage());
            return -1;
        }
    }

    public boolean isValidBuffer(int buffer) {
        return buffer != -1;
    }

    public int createSource(boolean loop) {
        int source = alGenSources();
        if (loop) {
            alSourcei(source, AL_LOOPING, AL_TRUE);
        }
        sources.add(source);
        return source;
    }

    public void playSound(int source, int buffer) {
        alSourceStop(source);
        alSourcei(source, AL_BUFFER, buffer);
        alSourcePlay(source);
    }

    public void stopSound(int source) {
        alSourceStop(source);
    }
    
    public void setVolume(int source, float volume) {
        alSourcef(source, AL_GAIN, volume);
    }

    public void cleanup() {
        for (int source : sources) {
            alDeleteSources(source);
        }
        for (int buffer : buffers) {
            alDeleteBuffers(buffer);
        }
        if (context != 0L) {
            alcDestroyContext(context);
        }
        if (device != 0L) {
            alcCloseDevice(device);
        }
    }
} 