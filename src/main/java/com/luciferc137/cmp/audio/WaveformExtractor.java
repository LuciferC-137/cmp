package com.luciferc137.cmp.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Extracts waveform data (RMS amplitude) from an audio file.
 * Returns an array of normalized values (0.0 to 1.0) representing
 * audio amplitude at regular intervals.
 */
public class WaveformExtractor {

    /** Default number of bins/samples for waveform display. */
    public static final int DEFAULT_NUM_BINS = 400;

    /**
     * Extracts the waveform from an audio file asynchronously.
     *
     * @param filePath path to the audio file
     * @param numSamples number of samples to return (waveform resolution)
     * @return CompletableFuture containing the normalized amplitude array
     */
    public CompletableFuture<float[]> extractAsync(String filePath, int numSamples) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extract(filePath, numSamples);
            } catch (Exception e) {
                System.err.println("Error extracting waveform: " + e.getMessage());
                return new float[numSamples];
            }
        });
    }

    /**
     * Extracts the waveform from an audio file.
     *
     * @param filePath path to the audio file
     * @param numSamples number of samples to return
     * @return array of normalized amplitudes (0.0 to 1.0)
     */
    public float[] extract(String filePath, int numSamples) throws UnsupportedAudioFileException, IOException {
        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            throw new IOException("Audio file not found: " + filePath);
        }

        List<Float> rmsValues = new ArrayList<>();

        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat baseFormat = audioInputStream.getFormat();

            // Convert to PCM if necessary (for MP3 files)
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream)) {
                int bytesPerFrame = decodedFormat.getFrameSize();
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize];
                int bytesRead;

                while ((bytesRead = decodedStream.read(buffer)) != -1) {
                    float rms = calculateRMSFromBytes(buffer, bytesRead, bytesPerFrame, decodedFormat.getChannels());
                    rmsValues.add(rms);
                }
            }
        }

        return resampleToSize(rmsValues, numSamples);
    }

    /**
     * Calculates the RMS (Root Mean Square) value from an audio byte buffer.
     */
    private float calculateRMSFromBytes(byte[] buffer, int bytesRead, int bytesPerFrame, int channels) {
        int samplesPerChannel = bytesRead / bytesPerFrame;
        if (samplesPerChannel == 0) return 0;

        double sum = 0;
        int sampleCount = 0;

        for (int i = 0; i < bytesRead - 1; i += 2) {
            // Convert 2 bytes to a 16-bit signed sample (little-endian)
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            // Normalize between -1 and 1
            float normalizedSample = sample / 32768.0f;
            sum += normalizedSample * normalizedSample;
            sampleCount++;
        }

        if (sampleCount == 0) return 0;
        return (float) Math.sqrt(sum / sampleCount);
    }

    /**
     * Resamples the list of RMS values to match the desired number
     * of samples (for progress bar resolution).
     */
    private float[] resampleToSize(List<Float> values, int targetSize) {
        if (values.isEmpty()) {
            return new float[targetSize];
        }

        float[] result = new float[targetSize];
        float maxValue = 0;

        // Find max value for normalization
        for (Float v : values) {
            if (v > maxValue) maxValue = v;
        }

        if (maxValue == 0) maxValue = 1; // Avoid division by zero

        float ratio = (float) values.size() / targetSize;

        for (int i = 0; i < targetSize; i++) {
            int startIdx = (int) (i * ratio);
            int endIdx = (int) ((i + 1) * ratio);
            endIdx = Math.min(endIdx, values.size());

            // Average values in this interval
            float sum = 0;
            int count = 0;
            for (int j = startIdx; j < endIdx; j++) {
                sum += values.get(j);
                count++;
            }

            if (count > 0) {
                result[i] = (sum / count) / maxValue; // Normalize between 0 and 1
            }
        }

        return result;
    }
}

