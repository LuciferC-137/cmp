#!/bin/bash
# Script pour installer les dépendances GStreamer manquantes

echo "Installation des dépendances GStreamer pour JavaFX..."
sudo apt-get update
sudo apt-get install -y gstreamer1.0-libav gstreamer1.0-plugins-ugly gstreamer1.0-plugins-bad

echo "Vérification des plugins installés:"
gst-inspect-1.0 | grep -i mp3

