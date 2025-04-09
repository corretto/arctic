package com.amazon.corretto.arctic.keycode;

import com.amazon.corretto.arctic.recorder.backend.converters.JnhNativeKeyEvent2ArcticEvent;
import com.amazon.corretto.arctic.recorder.backend.impl.JnhKeyboardRecorder;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class KeyTest {
    private final JnhKeyboardRecorder recorder;
    private boolean registeredHook = false;

    public static void main(final String... args) {
        try {
            new KeyTest();
        } catch (Exception e) {
            System.out.println("Error when running the keyboard test");
            e.printStackTrace();
        }
    }

    private KeyTest() {
        recorder = new JnhKeyboardRecorder(new JnhNativeKeyEvent2ArcticEvent());
        addShutdownHook();
        GlobalScreen.addNativeKeyListener(new KeyListener());
        registerHook();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
            @Override
            public void run() {
                unregisterHook();
            }
        });
    }

    private void registerHook() {
        if (!GlobalScreen.isNativeHookRegistered()) {
            try {
                GlobalScreen.registerNativeHook();
                registeredHook = true;
            } catch (final NativeHookException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void unregisterHook() {
        if (registeredHook) {
            try {
                GlobalScreen.unregisterNativeHook();
                registeredHook = false;
            } catch (final NativeHookException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class KeyListener implements NativeKeyListener {
        public void nativeKeyPressed(NativeKeyEvent e) {
            System.out.printf("Key code: %5d %s%n", e.getKeyCode(), NativeKeyEvent.getKeyText(e.getKeyCode()));
        }
    }
}
