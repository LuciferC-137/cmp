package com.luciferc137.cmp.ui.keyboard;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;


@DBusInterfaceName("org.mpris.MediaPlayer2")
public interface MediaPlayer2Root extends DBusInterface {
    void Raise();
    void Quit();
}
