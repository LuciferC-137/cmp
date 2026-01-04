package com.luciferc137.cmp.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Extracts waveform data (RMS amplitude) from an audio file.
 * Returns an array of normalized values (0.0 to 1.0) representing
 * audio amplitude at regular intervals.
 *
 * Supports multiple audio formats through Java Sound SPI providers.
 * Falls back to a synthetic waveform for unsupported formats.
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
                System.err.println("Waveform extraction failed for: " + filePath + " - " + e.getMessage());
                // Return a synthetic waveform as fallback
                return generateSyntheticWaveform(numSamples, filePath);
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

        AudioFormat format = AudioFormat.fromFile(audioFile);

        // Try Java Sound API first (works for MP3, WAV, and some other formats with SPI)
        try {
            return extractWithJavaSound(audioFile, numSamples);
        } catch (UnsupportedAudioFileException | IOException e) {
            // Format not supported by Java Sound, try alternative methods
            System.err.println("Java Sound API failed for " + format + ": " + e.getMessage());
        }

        // Try raw byte analysis for some formats
        try {
            return extractFromRawBytes(audioFile, numSamples, format);
        } catch (Exception e) {
            System.err.println("Raw byte extraction failed for " + format + ": " + e.getMessage());
        }

        // Fall back to synthetic waveform
        return generateSyntheticWaveform(numSamples, filePath);
    }

    /**
     * Extracts waveform using Java Sound API (works with MP3, WAV, OGG with SPI).
     */
    private float[] extractWithJavaSound(File audioFile, int numSamples) throws UnsupportedAudioFileException, IOException {
        List<Float> rmsValues = new ArrayList<>();

        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
            javax.sound.sampled.AudioFormat baseFormat = audioInputStream.getFormat();

            // Convert to PCM if necessary (for compressed formats like MP3)
            javax.sound.sampled.AudioFormat decodedFormat = new javax.sound.sampled.AudioFormat(
                    javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
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
                    float rms = calculateRMSFromBytes(buffer, bytesRead, bytesPerFrame);
                    rmsValues.add(rms);
                }
            }
        }

        if (rmsValues.isEmpty()) {
            throw new IOException("No audio data extracted");
        }

        return resampleToSize(rmsValues, numSamples);
    }

    /**
     * Attempts to extract waveform data by analyzing raw file bytes.
     * This is a fallback method for formats not supported by Java Sound.
     */
    private float[] extractFromRawBytes(File audioFile, int numSamples, AudioFormat format) throws IOException {
        // For M4A, FLAC, AAC - we can't easily decode without external libraries
        // But we can at least try to analyze file structure for some formats

        if (format == AudioFormat.FLAC) {
            return extractFromFlacRaw(audioFile, numSamples);
        }

        // For other unsupported formats, throw exception to trigger synthetic waveform
        throw new IOException("Format not supported for raw extraction: " + format);
    }

    /**
     * Basic FLAC waveform extraction by analyzing frame data.
     * This is a simplified approach that estimates amplitude from file structure.
     */
    private float[] extractFromFlacRaw(File audioFile, int numSamples) throws IOException {
        float[] result = new float[numSamples];
        long fileSize = audioFile.length();
        long chunkSize = fileSize / numSamples;

        try (RandomAccessFile raf = new RandomAccessFile(audioFile, "r")) {
            // Skip FLAC header (minimum 42 bytes for fLaC marker + STREAMINFO)
            long dataStart = Math.min(8192, fileSize / 10);

            for (int i = 0; i < numSamples; i++) {
                long position = dataStart + (i * chunkSize);
                if (position >= fileSize - 100) {
                    result[i] = result[Math.max(0, i - 1)];
                    continue;
                }

                raf.seek(position);
                byte[] chunk = new byte[(int) Math.min(512, chunkSize)];
                int read = raf.read(chunk);

                if (read > 0) {
                    // Calculate variance of bytes as amplitude estimate
                    result[i] = calculateByteVariance(chunk, read);
                }
            }
        }

        // Normalize
        float max = 0;
        for (float v : result) {
            if (v > max) max = v;
        }
        if (max > 0) {
            for (int i = 0; i < result.length; i++) {
                result[i] /= max;
            }
        }

        return result;
    }

    /**
     * Calculates a pseudo-amplitude value from raw byte data.
     */
    private float calculateByteVariance(byte[] data, int length) {
        if (length < 2) return 0;

        long sum = 0;
        long sumSq = 0;

        for (int i = 0; i < length; i++) {
            int val = data[i] & 0xFF;
            sum += val;
            sumSq += val * val;
        }

        double mean = (double) sum / length;
        double variance = (double) sumSq / length - mean * mean;

        return (float) Math.sqrt(variance) / 128f;
    }

    /**
     * Generates a synthetic waveform based on file hash for consistent display.
     * This provides visual feedback even when audio decoding is not possible.
     */
    private float[] generateSyntheticWaveform(int numSamples, String filePath) {
        float[] result = new float[numSamples];

        // Use file path hash for consistent pseudo-random pattern
        int seed = filePath.hashCode();
        Random random = new Random(seed);

        // Generate a smooth, natural-looking synthetic waveform
        float baseLevel = 0.3f + random.nextFloat() * 0.2f;
        float[] envelope = new float[numSamples];

        // Create envelope with smooth variations
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / numSamples;
            // Combine multiple sine waves for organic shape
            envelope[i] = (float) (
                0.5 +
                0.2 * Math.sin(t * Math.PI * 4 + random.nextFloat()) +
                0.15 * Math.sin(t * Math.PI * 8 + random.nextFloat() * 2) +
                0.1 * Math.sin(t * Math.PI * 16 + random.nextFloat() * 3)
            );
        }

        // Apply envelope and add detail
        for (int i = 0; i < numSamples; i++) {
            float detail = 0.8f + random.nextFloat() * 0.4f;
            result[i] = Math.min(1.0f, Math.max(0.1f, baseLevel * envelope[i] * detail));
        }

        return result;
    }

    /**
     * Calculates the RMS (Root Mean Square) value from an audio byte buffer.
     */
    private float calculateRMSFromBytes(byte[] buffer, int bytesRead, int bytesPerFrame) {
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

