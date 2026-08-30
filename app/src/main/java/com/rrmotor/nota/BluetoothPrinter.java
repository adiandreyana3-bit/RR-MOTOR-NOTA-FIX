package com.rrmotor.nota;

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

    // Bluetooth Serial Port Profile
    private static final UUID SPP_UUID =
            UUID.fromString(
                    "00001101-0000-1000-8000-00805F9B34FB"
            );

    public BluetoothPrinter(
            BluetoothDevice device) {

        this.device = device;
    }

    // =========================================================
    // CONNECT
    // =========================================================

    public void connect() throws IOException {

        if (device == null) {
            throw new IOException(
                    "Printer tidak ditemukan"
            );
        }

        socket =
                device.createRfcommSocketToServiceRecord(
                        SPP_UUID
                );

        socket.connect();

        outputStream =
                socket.getOutputStream();
    }

    // =========================================================
    // PRINT
    // =========================================================

    public void print(
            String teks) throws IOException {

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

        // ESC/POS reset printer
        outputStream.write(
                new byte[]{
                        0x1B,
                        0x40
                }
        );

        // Rata tengah
        outputStream.write(
                new byte[]{
                        0x1B,
                        0x61,
                        0x01
                }
        );

        // Teks nota
        byte[] data =
                teks.getBytes(
                        StandardCharsets.UTF_8
                );

        outputStream.write(data);

        // Rata kiri kembali
        outputStream.write(
                new byte[]{
                        0x1B,
                        0x61,
                        0x00
                }
        );

        // Feed beberapa baris
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
    // DISCONNECT
    // =========================================================

    public void disconnect() {

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
}
