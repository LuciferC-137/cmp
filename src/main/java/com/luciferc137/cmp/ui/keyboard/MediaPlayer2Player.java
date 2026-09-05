package com.luciferc137.cmp.ui.keyboard;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;


@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
public interface MediaPlayer2Player extends DBusInterface {
    void Next();
    void Previous();
    void Pause();
    void PlayPause();
    void Stop();
    void Play();
    void Seek(long offsetMicroseconds);
    void SetPosition(DBusPath trackId, long positionMicroseconds);
    void OpenUri(String uri);
}
