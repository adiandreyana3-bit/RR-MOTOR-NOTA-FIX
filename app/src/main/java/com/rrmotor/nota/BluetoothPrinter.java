package com.rrmotor.nota;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothPrinter {

    private final BluetoothDevice device;

    private BluetoothSocket socket;
    private OutputStream outputStream;

    // =========================================================
    // UUID BLUETOOTH SERIAL PORT PROFILE
    // =========================================================

    private static final UUID SPP_UUID =
            UUID.fromString(
                    "00001101-0000-1000-8000-00805F9B34FB"
            );

    // =========================================================
    // KONSTRUKTOR
    // =========================================================

    public BluetoothPrinter(BluetoothDevice device) {
        this.device = device;
    }

    // =========================================================
    // CONNECT
    // =========================================================

    public void connect() throws IOException {

        if (device == null) {
            throw new IOException(
                    "Printer Bluetooth tidak ditemukan"
            );
        }

        BluetoothAdapter adapter =
                BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            throw new IOException(
                    "Bluetooth tidak tersedia di HP"
            );
        }

        IOException errorTerakhir = null;

        // ---------------------------------------------------------
        // MATIKAN DISCOVERY
        // ---------------------------------------------------------

        try {
            adapter.cancelDiscovery();
        } catch (Exception ignored) {
        }

        // ---------------------------------------------------------
        // COBA SAMPAI 3 KALI
        // ---------------------------------------------------------

        for (int percobaan = 1;
             percobaan <= 3;
             percobaan++) {

            try {

                tutupSocket();

                // Pastikan discovery benar-benar berhenti
                try {
                    adapter.cancelDiscovery();
                } catch (Exception ignored) {
                }

                // -------------------------------------------------
                // COBA RFCOMM NORMAL
                // -------------------------------------------------

                try {

                    socket =
                            device.createRfcommSocketToServiceRecord(
                                    SPP_UUID
                            );

                    socket.connect();

                } catch (IOException normalError) {

                    // -------------------------------------------------
                    // JIKA GAGAL, COBA INSECURE RFCOMM
                    // -------------------------------------------------

                    tutupSocket();

                    try {
                        adapter.cancelDiscovery();
                    } catch (Exception ignored) {
                    }

                    socket =
                            device.createInsecureRfcommSocketToServiceRecord(
                                    SPP_UUID
                            );

                    socket.connect();
                }

                // -------------------------------------------------
                // AMBIL OUTPUT STREAM
                // -------------------------------------------------

                outputStream =
                        socket.getOutputStream();

                if (outputStream == null) {

                    throw new IOException(
                            "Output printer tidak tersedia"
                    );
                }

                // -------------------------------------------------
                // BERHASIL
                // -------------------------------------------------

                return;

            } catch (Exception e) {

                errorTerakhir =
                        new IOException(
                                e.getMessage(),
                                e
                        );

                tutupSocket();

                // Tunggu sebelum mencoba lagi
                if (percobaan < 3) {

                    try {

                        Thread.sleep(1000);

                    } catch (InterruptedException interruptedException) {

                        Thread.currentThread().interrupt();

                        throw new IOException(
                                "Koneksi printer dibatalkan",
                                interruptedException
                        );
                    }
                }
            }
        }

        // =========================================================
        // GAGAL SETELAH 3 KALI
        // =========================================================

        if (errorTerakhir != null) {

            String pesan =
                    errorTerakhir.getMessage();

            if (pesan == null ||
                    pesan.trim().isEmpty()) {

                pesan =
                        "Printer tidak merespons";
            }

            throw new IOException(
                    "Tidak dapat terhubung ke printer: "
                            + pesan
            );
        }

        throw new IOException(
                "Tidak dapat terhubung ke printer"
        );
    }

    // =========================================================
    // PRINT
    // =========================================================

    public void print(String teks) throws IOException {

        if (socket == null ||
                !socket.isConnected()) {

            throw new IOException(
                    "Printer belum terhubung"
            );
        }

        if (outputStream == null) {

            throw new IOException(
                    "Output printer tidak tersedia"
            );
        }

        if (teks == null) {
            teks = "";
        }

        // =========================================================
        // RESET PRINTER
        // =========================================================

        outputStream.write(
                new byte[]{
                        0x1B,
                        0x40
                }
        );

        // =========================================================
        // RATA KIRI
        // =========================================================

        outputStream.write(
                new byte[]{
                        0x1B,
                        0x61,
                        0x00
                }
        );

        // =========================================================
        // FONT NORMAL
        // =========================================================

        outputStream.write(
                new byte[]{
                        0x1B,
                        0x21,
                        0x00
                }
        );

        // =========================================================
        // CETAK TEKS
        // =========================================================
        //
        // Banyak printer thermal 58mm menggunakan CP437/ASCII.
        // Karakter emoji tidak selalu didukung printer thermal.
        //
        // Karena itu kita gunakan charset yang lebih aman.
        // =========================================================

        Charset charset =
                Charset.forName("windows-1252");

        byte[] data =
                teks.getBytes(charset);

        outputStream.write(data);

        // =========================================================
        // FEED KERTAS
        // =========================================================

        outputStream.write(
                new byte[]{
                        0x0A,
                        0x0A,
                        0x0A,
                        0x0A
                }
        );

        // =========================================================
        // POTONG KERTAS
        // =========================================================
        //
        // Tidak semua printer mendukung cutter.
        // Perintah ini aman untuk printer yang mendukungnya.
        // =========================================================

        try {

            outputStream.write(
                    new byte[]{
                            0x1D,
                            0x56,
                            0x00
                    }
            );

        } catch (Exception ignored) {
        }

        // =========================================================
        // PASTIKAN SEMUA DATA TERKIRIM
        // =========================================================

        outputStream.flush();

        // Beri sedikit waktu agar printer menerima data
        try {

            Thread.sleep(300);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    // =========================================================
    // TEST PRINT
    // =========================================================

    public void testPrint() throws IOException {

        if (socket == null ||
                !socket.isConnected()) {

            throw new IOException(
                    "Printer belum terhubung"
            );
        }

        if (outputStream == null) {

            throw new IOException(
                    "Output printer tidak tersedia"
            );
        }

        // Reset
        outputStream.write(
                new byte[]{
                        0x1B,
                        0x40
                }
        );

        // Tengah
        outputStream.write(
                new byte[]{
                        0x1B,
                        0x61,
                        0x01
                }
        );

        // Besarkan tulisan
        outputStream.write(
                new byte[]{
                        0x1D,
                        0x21,
                        0x11
                }
        );

        outputStream.write(
                "RR MOTOR\n"
                        .getBytes(
                                Charset.forName(
                                        "windows-1252"
                                )
                        )
        );

        // Kembali normal
        outputStream.write(
                new byte[]{
                        0x1D,
                        0x21,
                        0x00
                }
        );

        outputStream.write(
                "TEST PRINT\n"
                        .getBytes(
                                Charset.forName(
                                        "windows-1252"
                                )
                        )
        );

        outputStream.write(
                "\nBluetooth OK\n\n\n\n"
                        .getBytes(
                                Charset.forName(
                                        "windows-1252"
                                )
                        )
        );

        outputStream.flush();

        try {

            Thread.sleep(500);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    // =========================================================
    // CEK KONEKSI
    // =========================================================

    public boolean isConnected() {

        try {

            return socket != null &&
                    socket.isConnected() &&
                    outputStream != null;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // TUTUP SOCKET
    // =========================================================

    private void tutupSocket() {

        if (outputStream != null) {

            try {
                outputStream.flush();
            } catch (Exception ignored) {
            }

            try {
                outputStream.close();
            } catch (Exception ignored) {
            }
        }

        if (socket != null) {

            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }

        outputStream = null;
        socket = null;
    }

    // =========================================================
    // DISCONNECT
    // =========================================================

    public void disconnect() {

        tutupSocket();
    }
}
