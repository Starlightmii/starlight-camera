package com.starlight.camera;

import java.io.RandomAccessFile;

/** Minimal TIFF/EXIF orientation reader (works for DNG and JPEG/TIFF files). */
public final class StarlightTiff {
    private StarlightTiff() {}

    public static int orientation(String path) {
        if (path == null) return 0;
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(path, "r");
            if (f.length() < 8) return 0;
            int b0 = f.read();
            int b1 = f.read();
            boolean le;
            if (b0 == 0x49 && b1 == 0x49) le = true;
            else if (b0 == 0x4D && b1 == 0x4D) le = false;
            else return 0; // JPEG etc. — let ExifInterface handle those
            if (readU16(f, le) != 42) return 0;
            long ifd = readU32(f, le);
            if (ifd <= 0 || ifd >= f.length()) return 0;
            f.seek(ifd);
            int n = readU16(f, le);
            for (int i = 0; i < n; i++) {
                long pos = ifd + 2 + (long) i * 12;
                if (pos + 12 > f.length()) break;
                f.seek(pos);
                int tag = readU16(f, le);
                int type = readU16(f, le);
                long count = readU32(f, le);
                if (tag == 0x0112 && count >= 1) {
                    if (type == 3) return readU16(f, le);
                    if (type == 4) return (int) readU32(f, le);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (f != null) try { f.close(); } catch (Throwable ignored) {}
        }
        return 0;
    }

    private static int readU16(RandomAccessFile f, boolean le) throws Exception {
        int a = f.read();
        int b = f.read();
        if (a < 0 || b < 0) return 0;
        return le ? (a | (b << 8)) : ((a << 8) | b);
    }

    private static long readU32(RandomAccessFile f, boolean le) throws Exception {
        int a = f.read();
        int b = f.read();
        int c = f.read();
        int d = f.read();
        if ((a | b | c | d) < 0) return 0;
        long v = le
                ? ((long) a | ((long) b << 8) | ((long) c << 16) | ((long) d << 24))
                : (((long) a << 24) | ((long) b << 16) | ((long) c << 8) | (long) d);
        return v & 0xFFFFFFFFL;
    }
}
