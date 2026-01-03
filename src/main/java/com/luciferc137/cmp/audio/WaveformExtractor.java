package com.luciferc137.cmp.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Extrait les données de waveform (amplitude RMS) d'un fichier audio.
 * Retourne un tableau de valeurs normalisées (0.0 à 1.0) représentant
 * l'amplitude audio à intervalles réguliers.
 */
public class WaveformExtractor {

    /**
     * Extrait la waveform d'un fichier audio de manière asynchrone.
     *
     * @param filePath chemin du fichier audio
     * @param numSamples nombre de samples à retourner (résolution de la waveform)
     * @return CompletableFuture contenant le tableau des amplitudes normalisées
     */
    public CompletableFuture<float[]> extractAsync(String filePath, int numSamples) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extract(filePath, numSamples);
            } catch (Exception e) {
                System.err.println("Erreur lors de l'extraction de la waveform: " + e.getMessage());
                e.printStackTrace();
                return new float[numSamples];
            }
        });
    }

    /**
     * Extrait la waveform d'un fichier audio.
     *
     * @param filePath chemin du fichier audio
     * @param numSamples nombre de samples à retourner
     * @return tableau des amplitudes normalisées (0.0 à 1.0)
     */
    public float[] extract(String filePath, int numSamples) throws UnsupportedAudioFileException, IOException {
        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            throw new IOException("Fichier audio introuvable: " + filePath);
        }

        List<Float> rmsValues = new ArrayList<>();

        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat baseFormat = audioInputStream.getFormat();

            // Convertir en PCM si nécessaire (pour les fichiers MP3)
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
     * Calcule la valeur RMS (Root Mean Square) à partir d'un buffer de bytes audio.
     */
    private float calculateRMSFromBytes(byte[] buffer, int bytesRead, int bytesPerFrame, int channels) {
        int samplesPerChannel = bytesRead / bytesPerFrame;
        if (samplesPerChannel == 0) return 0;

        double sum = 0;
        int sampleCount = 0;

        for (int i = 0; i < bytesRead - 1; i += 2) {
            // Convertir 2 bytes en un échantillon 16-bit signé (little-endian)
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            // Normaliser entre -1 et 1
            float normalizedSample = sample / 32768.0f;
            sum += normalizedSample * normalizedSample;
            sampleCount++;
        }

        if (sampleCount == 0) return 0;
        return (float) Math.sqrt(sum / sampleCount);
    }

    /**
     * Rééchantillonne la liste de valeurs RMS pour correspondre au nombre
     * de samples désiré (pour la résolution de la barre de progression).
     */
    private float[] resampleToSize(List<Float> values, int targetSize) {
        if (values.isEmpty()) {
            return new float[targetSize];
        }

        float[] result = new float[targetSize];
        float maxValue = 0;

        // Trouver la valeur max pour normalisation
        for (Float v : values) {
            if (v > maxValue) maxValue = v;
        }

        if (maxValue == 0) maxValue = 1; // Éviter division par zéro

        float ratio = (float) values.size() / targetSize;

        for (int i = 0; i < targetSize; i++) {
            int startIdx = (int) (i * ratio);
            int endIdx = (int) ((i + 1) * ratio);
            endIdx = Math.min(endIdx, values.size());

            // Moyenne des valeurs dans cet intervalle
            float sum = 0;
            int count = 0;
            for (int j = startIdx; j < endIdx; j++) {
                sum += values.get(j);
                count++;
            }

            if (count > 0) {
                result[i] = (sum / count) / maxValue; // Normaliser entre 0 et 1
            }
        }

        return result;
    }
}

