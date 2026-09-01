package com.rrmotor.nota;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BluetoothPrinter {

    private final BluetoothDevice device;

    private BluetoothSocket socket;
    private OutputStream outputStream;

    // =========================================================
    // STANDARD BLUETOOTH SERIAL PORT PROFILE (SPP)
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
    // CONNECT DENGAN RETRY
    // =========================================================

    public void connect() throws IOException {

        if (device == null) {
            throw new IOException(
                    "Printer tidak ditemukan"
            );
        }

        IOException errorTerakhir = null;

        /*
         * Coba maksimal 3 kali.
         *
         * Banyak printer thermal Bluetooth kadang gagal
         * pada percobaan pertama walaupun sudah paired.
         */
        for (int percobaan = 1; percobaan <= 3; percobaan++) {

            try {

                tutupSocket();

                BluetoothAdapter adapter =
                        BluetoothAdapter.getDefaultAdapter();

                if (adapter != null) {

                    /*
                     * WAJIB dilakukan sebelum connect.
                     * Discovery Bluetooth dapat mengganggu
                     * atau memperlambat koneksi printer.
                     */
                    try {
                        adapter.cancelDiscovery();
                    } catch (Exception ignored) {
                    }
                }

                // =================================================
                // COBA KONEKSI NORMAL
                // =================================================

                try {

                    socket =
                            device.createRfcommSocketToServiceRecord(
                                    SPP_UUID
                            );

                    socket.connect();

                } catch (IOException normalError) {

                    /*
                     * Beberapa printer thermal murah/legacy
                     * lebih cocok menggunakan insecure RFCOMM.
                     *
                     * Karena itu kita coba metode kedua.
                     */

                    tutupSocket();

                    if (adapter != null) {
                        try {
                            adapter.cancelDiscovery();
                        } catch (Exception ignored) {
                        }
                    }

                    socket =
                            device.createInsecureRfcommSocketToServiceRecord(
                                    SPP_UUID
                            );

                    socket.connect();
                }

                // =================================================
                // AMBIL OUTPUT STREAM
                // =================================================

                outputStream =
                        socket.getOutputStream();

                if (outputStream == null) {

                    throw new IOException(
                            "Output printer tidak tersedia"
                    );
                }

                // =================================================
                // BERHASIL
                // =================================================

                return;

            } catch (Exception e) {

                errorTerakhir =
                        new IOException(
                                e.getMessage(),
                                e
                        );

                tutupSocket();

                /*
                 * Beri sedikit waktu sebelum percobaan berikutnya.
                 */
                if (percobaan < 3) {

                    try {
                        Thread.sleep(700);
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
        // SEMUA PERCOBAAN GAGAL
        // =========================================================

        if (errorTerakhir != null) {

            String pesan =
                    errorTerakhir.getMessage();

            if (pesan == null ||
                    pesan.trim().isEmpty()) {

                pesan =
                        "Tidak dapat terhubung ke printer";
            }

            throw new IOException(
                    "Gagal terhubung setelah 3 percobaan: "
                            + pesan,
                    errorTerakhir
            );
        }

        throw new IOException(
                "Gagal terhubung ke printer"
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
        // RATA TENGAH
        // =========================================================

        outputStream.write(
                new byte[]{
                        0x1B,
                        0x61,
                        0x01
                }
        );

        // =========================================================
        // CETAK TEKS
        // =========================================================

        if (teks == null) {
            teks = "";
        }

        byte[] data =
                teks.getBytes(
                        StandardCharsets.UTF_8
                );

        outputStream.write(data);

        // =========================================================
        // KEMBALI KE RATA KIRI
        // =========================================================

        outputStream.write(
                new byte[]{
                        0x1B,
                        0x61,
                        0x00
                }
        );

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

        outputStream.flush();
    }

    // =========================================================
    // CEK TERHUBUNG
    // =========================================================

    public boolean isConnected() {

        try {

            return socket != null &&
                    socket.isConnected();

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // TUTUP SOCKET
    // =========================================================

    private void tutupSocket() {

        try {

            if (outputStream != null) {
                outputStream.close();
            }

        } catch (Exception ignored) {
        }

        try {

            if (socket != null) {
                socket.close();
            }

        } catch (Exception ignored) {
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
