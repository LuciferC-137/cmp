#!/bin/bash
# GStreamer dependencies for JavaFX installation script

echo "Installing JavaFX dependencies..."
sudo apt-get update
sudo apt-get install -y gstreamer1.0-libav gstreamer1.0-plugins-ugly gstreamer1.0-plugins-bad

echo "Verifying installed plugins:"
gst-inspect-1.0 | grep -i mp3

