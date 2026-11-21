package com.arnestyles.client.mods;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * Windows-Medien-Schnittstelle (System Media Transport Controls, wie das Medien-Fenster von Windows):
 * Titel, Kuenstler, genaue Position, Laenge, Pause und Cover des laufenden Songs. Rein lokal, ohne Internet.
 * Direkt ueber WinRT/COM mit JNA (kein PowerShell, keine Zusatz-Programme). Nur von einem eigenen Thread aufrufen.
 */
public final class WinMedia {
    /** Momentaufnahme. positionMs gilt zum Zeitpunkt atMillis (System.currentTimeMillis). status 4 = spielt, 5 = Pause. */
    public record Info(String appId, String title, String artist, String album, int status, long positionMs, long atMillis,
                       long durationMs) {
        public boolean playing() {
            return status == 4;
        }

        /** Position jetzt (laeuft beim Abspielen weiter). */
        public long positionNow() {
            return playing() ? positionMs + Math.max(0, System.currentTimeMillis() - atMillis) : positionMs;
        }
    }

    private static final int E_ILLEGAL_METHOD_CALL = 0x8000000E;
    private static final long FILETIME_EPOCH_DIFF = 116444736000000000L;
    private static final int MAX_THUMBNAIL_BYTES = 8 * 1024 * 1024;
    private static final Guid.GUID IID_MANAGER_STATICS = new Guid.GUID("{2050C4EE-11A0-57DE-AED7-C97C70338245}");
    private static final Guid.GUID IID_ISTREAM = new Guid.GUID("{0000000C-0000-0000-C000-000000000046}");

    private static boolean initialized;
    private static @Nullable Pointer manager;
    /** Session des letzten poll (fuer das Cover), wird beim naechsten poll freigegeben. */
    private static @Nullable Pointer lastSession;
    private static @Nullable Pointer lastProperties;

    private WinMedia() {
    }

    // ---- WinRT-Grundlagen ----

    private static int vcall(Pointer obj, int index, Object... args) {
        Pointer vtable = obj.getPointer(0);
        Function f = Function.getFunction(vtable.getPointer((long) index * Native.POINTER_SIZE));
        Object[] all = new Object[args.length + 1];
        all[0] = obj;
        System.arraycopy(args, 0, all, 1, args.length);
        return f.invokeInt(all);
    }

    private static void release(@Nullable Pointer obj) {
        if (obj != null) {
            vcall(obj, 2);
        }
    }

    private static Pointer hstring(String value) {
        PointerByReference out = new PointerByReference();
        int hr = Function.getFunction("combase", "WindowsCreateString").invokeInt(new Object[]{new WString(value), value.length(), out});
        if (hr != 0) {
            throw new IllegalStateException("WindowsCreateString " + Integer.toHexString(hr));
        }
        return out.getValue();
    }

    /** Liest einen HSTRING und gibt ihn frei. */
    private static String readHString(@Nullable Pointer h) {
        if (h == null) {
            return "";
        }
        IntByReference length = new IntByReference();
        Pointer buffer = (Pointer) Function.getFunction("combase", "WindowsGetStringRawBuffer").invoke(Pointer.class, new Object[]{h, length});
        String value = buffer == null || length.getValue() <= 0 ? "" : new String(buffer.getCharArray(0, Math.min(length.getValue(), 2048)));
        Function.getFunction("combase", "WindowsDeleteString").invokeInt(new Object[]{h});
        return value;
    }

    /** Wartet auf eine WinRT-Async-Operation (GetResults meldet E_ILLEGAL_METHOD_CALL, solange sie laeuft). */
    private static @Nullable Pointer await(@Nullable Pointer operation, long timeoutMs) throws InterruptedException {
        if (operation == null) {
            return null;
        }
        try {
            long end = System.currentTimeMillis() + timeoutMs;
            PointerByReference result = new PointerByReference();
            while (true) {
                int hr = vcall(operation, 8, result);
                if (hr == 0) {
                    return result.getValue();
                }
                if (hr != E_ILLEGAL_METHOD_CALL || System.currentTimeMillis() > end) {
                    return null;
                }
                Thread.sleep(5);
            }
        } finally {
            release(operation);
        }
    }

    private static @Nullable Pointer out(Pointer obj, int index) {
        PointerByReference out = new PointerByReference();
        return vcall(obj, index, out) == 0 ? out.getValue() : null;
    }

    private static String outString(Pointer obj, int index) {
        PointerByReference out = new PointerByReference();
        return vcall(obj, index, out) == 0 ? readHString(out.getValue()) : "";
    }

    private static long outLong(Pointer obj, int index) {
        LongByReference out = new LongByReference();
        return vcall(obj, index, out) == 0 ? out.getValue() : 0L;
    }

    private static boolean init() throws InterruptedException {
        if (initialized) {
            return manager != null;
        }
        initialized = true;
        // Multithreaded Apartment fuer diesen Thread (1 = RO_INIT_MULTITHREADED)
        Function.getFunction("combase", "RoInitialize").invokeInt(new Object[]{1});
        Pointer classId = hstring("Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager");
        PointerByReference factory = new PointerByReference();
        int hr;
        try {
            hr = Function.getFunction("combase", "RoGetActivationFactory").invokeInt(new Object[]{classId, IID_MANAGER_STATICS, factory});
        } finally {
            Function.getFunction("combase", "WindowsDeleteString").invokeInt(new Object[]{classId});
        }
        if (hr != 0 || factory.getValue() == null) {
            return false;
        }
        try {
            // IGlobalSystemMediaTransportControlsSessionManagerStatics.RequestAsync
            manager = await(out(factory.getValue(), 6), 3000);
        } finally {
            release(factory.getValue());
        }
        return manager != null;
    }

    // ---- Abfrage ----

    /**
     * Aktueller Song. Mit onlySpotify nur die Spotify-Session, sonst Spotify bevorzugt und sonst die aktuelle Session
     * (z. B. Browser). null, wenn nichts laeuft oder die Schnittstelle fehlt.
     */
    public static synchronized @Nullable Info poll(boolean onlySpotify) throws InterruptedException {
        release(lastProperties);
        release(lastSession);
        lastProperties = null;
        lastSession = null;
        if (!init() || manager == null) {
            return null;
        }
        Pointer session = null;
        Pointer sessions = out(manager, 7);
        if (sessions != null) {
            try {
                IntByReference size = new IntByReference();
                vcall(sessions, 7, size);
                for (int i = 0; i < Math.min(size.getValue(), 32); i++) {
                    PointerByReference item = new PointerByReference();
                    if (vcall(sessions, 6, i, item) != 0 || item.getValue() == null) {
                        continue;
                    }
                    String app = outString(item.getValue(), 6).toLowerCase(Locale.ROOT);
                    if (session == null && app.contains("spotify")) {
                        session = item.getValue();
                    } else {
                        release(item.getValue());
                    }
                }
            } finally {
                release(sessions);
            }
        }
        if (session == null && !onlySpotify) {
            session = out(manager, 6);
        }
        if (session == null) {
            return null;
        }
        String appId = outString(session, 6);
        Pointer properties = await(out(session, 7), 2000);
        if (properties == null) {
            release(session);
            return null;
        }
        String title = outString(properties, 6);
        String artist = outString(properties, 9);
        if (artist.isEmpty()) {
            artist = outString(properties, 8);
        }
        String album = outString(properties, 10);

        long positionMs = 0;
        long atMillis = System.currentTimeMillis();
        long durationMs = 0;
        Pointer timeline = out(session, 8);
        if (timeline != null) {
            long start = outLong(timeline, 6);
            long end = outLong(timeline, 7);
            long position = outLong(timeline, 10);
            long updated = outLong(timeline, 11);
            durationMs = Math.max(0, (end - start) / 10_000L);
            positionMs = Math.max(0, (position - start) / 10_000L);
            if (updated > FILETIME_EPOCH_DIFF) {
                atMillis = (updated - FILETIME_EPOCH_DIFF) / 10_000L;
            }
            release(timeline);
        }
        int status = 0;
        Pointer playback = out(session, 9);
        if (playback != null) {
            IntByReference value = new IntByReference();
            if (vcall(playback, 7, value) == 0) {
                status = value.getValue();
            }
            release(playback);
        }
        lastSession = session;
        lastProperties = properties;
        return new Info(appId, title, artist, album, status, positionMs, atMillis, durationMs);
    }

    /** Steuer-Befehle fuer {@link #control}: Index in der Session-Vtable (TryTogglePlayPauseAsync usw.). */
    public static final int TOGGLE = 20;
    public static final int NEXT = 16;
    public static final int PREVIOUS = 17;

    /**
     * Schickt Play/Pause, naechster oder vorheriger Song an die zuletzt abgefragte Session (wie die Medientasten).
     * true, wenn Windows den Befehl angenommen hat.
     */
    public static synchronized boolean control(int command) throws InterruptedException {
        if (lastSession == null || (command != TOGGLE && command != NEXT && command != PREVIOUS)) {
            return false;
        }
        // IAsyncOperation<bool>: Ergebnis ist ein boolean, kein Objekt
        PointerByReference operation = new PointerByReference();
        if (vcall(lastSession, command, operation) != 0 || operation.getValue() == null) {
            return false;
        }
        Pointer op = operation.getValue();
        try {
            long end = System.currentTimeMillis() + 2000;
            IntByReference ok = new IntByReference();
            while (true) {
                int hr = vcall(op, 8, ok);
                if (hr == 0) {
                    return (ok.getValue() & 0xFF) != 0;
                }
                if (hr != E_ILLEGAL_METHOD_CALL || System.currentTimeMillis() > end) {
                    return false;
                }
                Thread.sleep(5);
            }
        } finally {
            release(op);
        }
    }

    /** Cover-Bild (PNG oder JPEG) des zuletzt abgefragten Songs, oder null. */
    public static synchronized byte @Nullable [] thumbnail() throws InterruptedException {
        if (lastProperties == null) {
            return null;
        }
        Pointer reference = out(lastProperties, 15);
        if (reference == null) {
            return null;
        }
        Pointer stream = null;
        Pointer comStream = null;
        try {
            stream = await(out(reference, 6), 3000);
            if (stream == null) {
                return null;
            }
            PointerByReference converted = new PointerByReference();
            int hr = Function.getFunction("shcore", "CreateStreamOverRandomAccessStream").invokeInt(new Object[]{stream, IID_ISTREAM, converted});
            if (hr != 0 || converted.getValue() == null) {
                return null;
            }
            comStream = converted.getValue();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            Memory buffer = new Memory(65536);
            IntByReference read = new IntByReference();
            while (bytes.size() < MAX_THUMBNAIL_BYTES) {
                // IStream::Read
                int r = vcall(comStream, 3, buffer, 65536, read);
                if ((r != 0 && r != 1) || read.getValue() <= 0) {
                    break;
                }
                bytes.write(buffer.getByteArray(0, read.getValue()), 0, read.getValue());
            }
            return bytes.size() == 0 || bytes.size() >= MAX_THUMBNAIL_BYTES ? null : bytes.toByteArray();
        } finally {
            release(comStream);
            release(stream);
            release(reference);
        }
    }
}
