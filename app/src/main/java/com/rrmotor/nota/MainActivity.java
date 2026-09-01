package com.rrmotor.nota;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.Calendar;

public class MainActivity extends Activity {

    private static final int REQUEST_BLUETOOTH = 1001;

    private EditText namaInput;
    private EditText waInput;
    private EditText tanggalInput;
    private EditText motorInput;
    private EditText dpInput;

    private Spinner statusSpinner;

    private LinearLayout itemContainer;
    private ScrollView scrollView;

    private TextView totalText;
    private TextView sisaText;
    private TextView statusText;

    private final ArrayList<EditText> namaBarang =
            new ArrayList<>();

    private final ArrayList<EditText> jumlahBarang =
            new ArrayList<>();

    private final ArrayList<EditText> hargaBarang =
            new ArrayList<>();

    private static final String PREF_NAME =
            "RR_MOTOR_NOTA";

    private static final String KEY_HISTORY =
            "HISTORY";

    private static final long SATU_TAHUN =
            365L * 24L * 60L * 60L * 1000L;

    /*
     * Jika nilainya bukan 0 berarti sedang mengedit
     * nota dari riwayat.
     */
    private long editingTimestamp = 0;

    /*
     * Status pembayaran:
     * LUNAS
     * BELUM LUNAS
     */
    private String statusBayar = "BELUM LUNAS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        bersihkanRiwayatLama();

        tampilkanMenuUtama();
    }

    // =========================================================
    // MENU UTAMA
    // =========================================================

    private void tampilkanMenuUtama() {

        editingTimestamp = 0;

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout utama =
                new LinearLayout(this);

        utama.setOrientation(
                LinearLayout.VERTICAL
        );

        utama.setPadding(
                20,
                15,
                20,
                40
        );

        scrollView.addView(utama);

        // =====================================================
        // JUDUL
        // =====================================================

        TextView judul =
                new TextView(this);

        judul.setText(
                "🏍️ RR MOTOR NOTA"
        );

        judul.setTextSize(23);
        judul.setTypeface(
                null,
                Typeface.BOLD
        );

        judul.setGravity(
                Gravity.CENTER
        );

        judul.setPadding(
                0,
                5,
                0,
                12
        );

        utama.addView(judul);

        // =====================================================
        // DATA PELANGGAN
        // =====================================================

        namaInput =
                buatInput(
                        "Nama pelanggan *"
                );

        namaInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );

        utama.addView(namaInput);

        waInput =
                buatInput(
                        "Nomor WhatsApp *"
                );

        waInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        utama.addView(waInput);

        tanggalInput =
                buatInput(
                        "Tanggal nota *"
                );

        tanggalInput.setFocusable(false);
        tanggalInput.setClickable(true);

        tanggalInput.setOnClickListener(
                v -> pilihTanggal()
        );

        utama.addView(tanggalInput);

        motorInput =
                buatInput(
                        "Jenis motor (opsional)"
                );

        motorInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );

        utama.addView(motorInput);

        dpInput =
                buatInput(
                        "DP / Uang muka (opsional)"
                );

        dpInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        utama.addView(dpInput);

        // =====================================================
        // STATUS PEMBAYARAN
        // =====================================================

        TextView statusLabel =
                new TextView(this);

        statusLabel.setText(
                "Status pembayaran"
        );

        statusLabel.setTextSize(16);
        statusLabel.setTypeface(
                null,
                Typeface.BOLD
        );

        statusLabel.setPadding(
                5,
                8,
                5,
                3
        );

        utama.addView(statusLabel);

        statusSpinner =
                new Spinner(this);

        String[] pilihanStatus = {
                "BELUM LUNAS",
                "LUNAS"
        };

        ArrayAdapter<String> adapterStatus =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        pilihanStatus
                );

        statusSpinner.setAdapter(
                adapterStatus
        );

        utama.addView(statusSpinner);

        statusSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        statusBayar =
                                position == 1
                                        ? "LUNAS"
                                        : "BELUM LUNAS";

                        perbaruiTampilanStatus();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        // =====================================================
        // DAFTAR BARANG
        // =====================================================

        TextView judulItem =
                new TextView(this);

        judulItem.setText(
                "🧾 DAFTAR BARANG / JASA"
        );

        judulItem.setTextSize(18);

        judulItem.setTypeface(
                null,
                Typeface.BOLD
        );

        judulItem.setPadding(
                0,
                18,
                0,
                7
        );

        utama.addView(judulItem);

        itemContainer =
                new LinearLayout(this);

        itemContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        utama.addView(
                itemContainer
        );

        Button tambahItem =
                new Button(this);

        tambahItem.setText(
                "＋ TAMBAH BARANG / JASA"
        );

        tambahItem.setTextSize(14);

        tambahItem.setOnClickListener(
                v -> tambahBarisItem()
        );

        utama.addView(
                tambahItem
        );

        // =====================================================
        // TOTAL
        // =====================================================

        totalText =
                new TextView(this);

        totalText.setTextSize(18);

        totalText.setTypeface(
                null,
                Typeface.BOLD
        );

        totalText.setPadding(
                0,
                12,
                0,
                3
        );

        utama.addView(
                totalText
        );

        sisaText =
                new TextView(this);

        sisaText.setTextSize(18);

        sisaText.setTypeface(
                null,
                Typeface.BOLD
        );

        utama.addView(
                sisaText
        );

        statusText =
                new TextView(this);

        statusText.setTextSize(16);

        statusText.setTypeface(
                null,
                Typeface.BOLD
        );

        statusText.setPadding(
                0,
                3,
                0,
                10
        );

        utama.addView(
                statusText
        );

        // =====================================================
        // SIMPAN
        // =====================================================

        Button simpan =
                new Button(this);

        simpan.setText(
                "💾 SIMPAN NOTA"
        );

        simpan.setTextSize(14);

        simpan.setOnClickListener(
                v -> simpanNota()
        );

        utama.addView(
                simpan
        );

        // =====================================================
        // CETAK BLUETOOTH
        // =====================================================

        Button cetakBluetooth =
                new Button(this);

        cetakBluetooth.setText(
                "🔵🖨️ CETAK BLUETOOTH"
        );

        cetakBluetooth.setTextSize(15);

        cetakBluetooth.setOnClickListener(
                v -> mulaiCetakBluetooth()
        );

        utama.addView(
                cetakBluetooth
        );

        // =====================================================
        // CETAK PDF
        // =====================================================

        Button cetak =
                new Button(this);

        cetak.setText(
                "🖨️ CETAK NOTA / PDF"
        );

        cetak.setTextSize(14);

        cetak.setOnClickListener(
                v -> cetakNota()
        );

        utama.addView(
                cetak
        );

        // =====================================================
        // RIWAYAT
        // =====================================================

        Button riwayat =
                new Button(this);

        riwayat.setText(
                "📋 RIWAYAT NOTA"
        );

        riwayat.setTextSize(14);

        riwayat.setOnClickListener(
                v -> tampilkanRiwayat()
        );

        utama.addView(
                riwayat
        );

        // =====================================================
        // WHATSAPP
        // =====================================================

        Button whatsapp =
                new Button(this);

        whatsapp.setText(
                "💬 KIRIM VIA WHATSAPP"
        );

        whatsapp.setTextSize(14);

        whatsapp.setOnClickListener(
                v -> kirimWhatsApp()
        );

        utama.addView(
                whatsapp
        );

        // =====================================================
        // SET CONTENT
        // =====================================================

        setContentView(
                scrollView
        );

        // =====================================================
        // TANGGAL OTOMATIS
        // =====================================================

        tanggalInput.setText(
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                ).format(
                        new Date()
                )
        );

        // =====================================================
        // ITEM PERTAMA
        // =====================================================

        tambahBarisItem();

        pasangListenerHitung(
                dpInput
        );

        statusBayar =
                "BELUM LUNAS";

        statusSpinner.setSelection(0);

        hitungTotal();
    }

    // =========================================================
    // INPUT
    // =========================================================

    private EditText buatInput(
            String hint) {

        EditText input =
                new EditText(this);

        input.setHint(hint);
        input.setTextSize(16);
        input.setSingleLine(true);

        input.setFocusable(true);
        input.setFocusableInTouchMode(true);
        input.setClickable(true);
        input.setLongClickable(true);

        input.setPadding(
                15,
                8,
                15,
                8
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                3,
                0,
                3
        );

        input.setLayoutParams(
                params
        );

        return input;
    }

    // =========================================================
    // LISTENER HITUNG
    // =========================================================

    private void pasangListenerHitung(
            EditText input) {

        input.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        hitungTotal();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );
    }

    // =========================================================
    // TANGGAL
    // =========================================================

    private void pilihTanggal() {

        Calendar kalender =
                Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view,
                         year,
                         month,
                         day) -> {

                            String tanggal =
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d/%02d/%04d",
                                            day,
                                            month + 1,
                                            year
                                    );

                            tanggalInput.setText(
                                    tanggal
                            );
                        },
                        kalender.get(
                                Calendar.YEAR
                        ),
                        kalender.get(
                                Calendar.MONTH
                        ),
                        kalender.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        dialog.show();
    }

    // =========================================================
    // TAMBAH ITEM
    // =========================================================

    private void tambahBarisItem() {

        LinearLayout baris =
                new LinearLayout(this);

        baris.setOrientation(
                LinearLayout.VERTICAL
        );

        baris.setPadding(
                0,
                5,
                0,
                5
        );

        EditText nama =
                buatInput(
                        "Nama barang / jasa"
                );

        nama.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        EditText jumlah =
                buatInput(
                        "Jumlah"
                );

        jumlah.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        EditText harga =
                buatInput(
                        "Harga satuan"
                );

        harga.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        Button hapus =
                new Button(this);

        hapus.setText(
                "🗑️ HAPUS ITEM"
        );

        hapus.setTextSize(13);

        baris.addView(nama);
        baris.addView(jumlah);
        baris.addView(harga);
        baris.addView(hapus);

        itemContainer.addView(
                baris
        );

        namaBarang.add(nama);
        jumlahBarang.add(jumlah);
        hargaBarang.add(harga);

        pasangListenerHitung(
                jumlah
        );

        pasangListenerHitung(
                harga
        );

        hapus.setOnClickListener(
                v -> {

                    int posisi =
                            namaBarang.indexOf(
                                    nama
                            );

                    if (namaBarang.size() > 1) {

                        if (posisi >= 0) {

                            namaBarang.remove(
                                    posisi
                            );

                            jumlahBarang.remove(
                                    posisi
                            );

                            hargaBarang.remove(
                                    posisi
                            );

                            itemContainer.removeView(
                                    baris
                            );

                            hitungTotal();
                        }

                    } else {

                        Toast.makeText(
                                this,
                                "Minimal harus ada 1 item",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    // =========================================================
    // ANGKA
    // =========================================================

    private long angka(
            EditText input) {

        if (input == null) {
            return 0;
        }

        try {

            String teks =
                    input.getText()
                            .toString()
                            .replace(
                                    ".",
                                    ""
                            )
                            .replace(
                                    ",",
                                    ""
                            )
                            .trim();

            if (teks.isEmpty()) {
                return 0;
            }

            return Long.parseLong(
                    teks
            );

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // HITUNG TOTAL
    // =========================================================

    private long hitungTotal() {

        long total = 0;

        if (namaBarang != null) {

            for (int i = 0;
                 i < namaBarang.size();
                 i++) {

                long jumlah =
                        angka(
                                jumlahBarang.get(i)
                        );

                long harga =
                        angka(
                                hargaBarang.get(i)
                        );

                if (jumlah < 0) {
                    jumlah = 0;
                }

                if (harga < 0) {
                    harga = 0;
                }

                total +=
                        jumlah * harga;
            }
        }

        long dp =
                angka(dpInput);

        if (dp < 0) {
            dp = 0;
        }

        long sisa =
                total - dp;

        if (sisa < 0) {
            sisa = 0;
        }

        if (totalText != null) {

            totalText.setText(
                    "TOTAL : " +
                            formatRupiah(total)
            );
        }

        if (sisaText != null) {

            sisaText.setText(
                    "SISA : " +
                            formatRupiah(sisa)
            );
        }

        perbaruiTampilanStatus();

        return total;
    }

    // =========================================================
    // STATUS
    // =========================================================

    private void perbaruiTampilanStatus() {

        if (statusText == null) {
            return;
        }

        if (statusBayar == null ||
                statusBayar.isEmpty()) {

            long total =
                    hitungTotalTanpaStatus();

            long dp =
                    angka(dpInput);

            if (dp >= total) {
                statusBayar =
                        "LUNAS";
            } else {
                statusBayar =
                        "BELUM LUNAS";
            }
        }

        if ("LUNAS".equals(
                statusBayar
        )) {

            statusText.setText(
                    "STATUS : LUNAS ✅"
            );

        } else {

            statusText.setText(
                    "STATUS : BELUM LUNAS ⚠️"
            );
        }
    }

    private long hitungTotalTanpaStatus() {

        long total = 0;

        if (namaBarang == null) {
            return 0;
        }

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            long jumlah =
                    angka(
                            jumlahBarang.get(i)
                    );

            long harga =
                    angka(
                            hargaBarang.get(i)
                    );

            total +=
                    jumlah * harga;
        }

        return total;
    }

    // =========================================================
    // RUPIAH
    // =========================================================

    private String formatRupiah(
            long angka) {

        NumberFormat format =
                NumberFormat.getNumberInstance(
                        new Locale(
                                "id",
                                "ID"
                        )
                );

        return "Rp " +
                format.format(angka);
    }

    // =========================================================
    // SIMPAN / UPDATE NOTA
    // =========================================================

    private void simpanNota() {

        if (!validasiNota()) {
            return;
        }

        String nama =
                namaInput.getText()
                        .toString()
                        .trim();

        String wa =
                waInput.getText()
                        .toString()
                        .trim();

        String tanggal =
                tanggalInput.getText()
                        .toString()
                        .trim();

        String motor =
                motorInput.getText()
                        .toString()
                        .trim();

        hitungTotal();

        StringBuilder dataItem =
                new StringBuilder();

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            String barang =
                    namaBarang.get(i)
                            .getText()
                            .toString()
                            .trim();

            long jumlah =
                    angka(
                            jumlahBarang.get(i)
                    );

            long harga =
                    angka(
                            hargaBarang.get(i)
                    );

            if (!barang.isEmpty()) {

                if (dataItem.length() > 0) {
                    dataItem.append(";");
                }

                dataItem.append(
                        encode(barang)
                )
                .append("~")
                .append(jumlah)
                .append("~")
                .append(harga);
            }
        }

        if (dataItem.length() == 0) {

            Toast.makeText(
                    this,
                    "Masukkan minimal 1 barang atau jasa",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        SharedPreferences pref =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String riwayat =
                pref.getString(
                        KEY_HISTORY,
                        ""
                );

        /*
         * Jika sedang edit:
         * gunakan timestamp lama.
         *
         * Jika nota baru:
         * gunakan timestamp sekarang.
         */
        long waktu;

        if (editingTimestamp != 0) {
            waktu = editingTimestamp;
        } else {
            waktu =
                    System.currentTimeMillis();
        }

        String data =
                waktu + "|" +
                        encode(nama) + "|" +
                        encode(wa) + "|" +
                        encode(tanggal) + "|" +
                        encode(motor) + "|" +
                        angka(dpInput) + "|" +
                        encode(statusBayar) + "|" +
                        dataItem;

        if (editingTimestamp != 0) {

            String[] semua =
                    riwayat.isEmpty()
                            ? new String[0]
                            : riwayat.split(
                                    "\n"
                            );

            StringBuilder hasil =
                    new StringBuilder();

            boolean ditemukan = false;

            for (String nota :
                    semua) {

                if (nota == null ||
                        nota.trim().isEmpty()) {
                    continue;
                }

                String[] bagian =
                        nota.split(
                                "\\|",
                                8
                        );

                boolean sama = false;

                if (bagian.length > 0) {

                    try {

                        long waktuLama =
                                Long.parseLong(
                                        bagian[0]
                                );

                        sama =
                                waktuLama ==
                                        editingTimestamp;

                    } catch (Exception ignored) {
                    }
                }

                if (sama) {

                    if (hasil.length() > 0) {
                        hasil.append("\n");
                    }

                    hasil.append(data);

                    ditemukan = true;

                } else {

                    if (hasil.length() > 0) {
                        hasil.append("\n");
                    }

                    hasil.append(nota);
                }
            }

            if (!ditemukan) {

                if (hasil.length() > 0) {
                    hasil.append("\n");
                }

                hasil.append(data);
            }

            pref.edit()
                    .putString(
                            KEY_HISTORY,
                            hasil.toString()
                    )
                    .apply();

            editingTimestamp = 0;

            Toast.makeText(
                    this,
                    "Nota berhasil diperbarui ✅",
                    Toast.LENGTH_LONG
            ).show();

        } else {

            if (!riwayat.isEmpty()) {
                riwayat += "\n";
            }

            riwayat += data;

            pref.edit()
                    .putString(
                            KEY_HISTORY,
                            riwayat
                    )
                    .apply();

            Toast.makeText(
                    this,
                    "Nota berhasil disimpan ✅",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // ENCODE
    // =========================================================

    private String encode(
            String teks) {

        if (teks == null) {
            return "";
        }

        return teks
                .replace(
                        "|",
                        "%7C"
                )
                .replace(
                        ";",
                        "%3B"
                )
                .replace(
                        "~",
                        "%7E"
                )
                .replace(
                        "\n",
                        " "
                )
                .replace(
                        "\r",
                        " "
                );
    }

    // =========================================================
    // DECODE
    // =========================================================

    private String decode(
            String teks) {

        if (teks == null) {
            return "";
        }

        return teks
                .replace(
                        "%7C",
                        "|"
                )
                .replace(
                        "%3B",
                        ";"
                )
                .replace(
                        "%7E",
                        "~"
                );
    }

    // =========================================================
    // BERSIHKAN RIWAYAT > 1 TAHUN
    // =========================================================

    private void bersihkanRiwayatLama() {

        SharedPreferences pref =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String data =
                pref.getString(
                        KEY_HISTORY,
                        ""
                );

        if (data.isEmpty()) {
            return;
        }

        String[] semua =
                data.split("\n");

        long sekarang =
                System.currentTimeMillis();

        StringBuilder hasil =
                new StringBuilder();

        for (String nota :
                semua) {

            try {

                String[] bagian =
                        nota.split(
                                "\\|",
                                8
                        );

                if (bagian.length < 7) {
                    continue;
                }

                long waktu =
                        Long.parseLong(
                                bagian[0]
                        );

                if (sekarang - waktu <=
                        SATU_TAHUN) {

                    if (hasil.length() > 0) {
                        hasil.append("\n");
                    }

                    hasil.append(nota);
                }

            } catch (Exception ignored) {
            }
        }

        pref.edit()
                .putString(
                        KEY_HISTORY,
                        hasil.toString()
                )
                .apply();
    }

    // =========================================================
    // RIWAYAT NOTA
    // =========================================================

    private void tampilkanRiwayat() {

        bersihkanRiwayatLama();

        final ArrayList<String> hasilCari =
                new ArrayList<>();

        final ArrayList<Long> timestampCari =
                new ArrayList<>();

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                20,
                10,
                20,
                10
        );

        // =====================================================
        // PENCARIAN
        // =====================================================

        EditText cariInput =
                new EditText(this);

        cariInput.setHint(
                "🔎 Cari nama / nomor WhatsApp"
        );

        cariInput.setTextSize(16);

        cariInput.setSingleLine(true);

        layout.addView(
                cariInput
        );

        Button tombolCari =
                new Button(this);

        tombolCari.setText(
                "🔎 CARI RIWAYAT"
        );

        layout.addView(
                tombolCari
        );

        LinearLayout daftar =
                new LinearLayout(this);

        daftar.setOrientation(
                LinearLayout.VERTICAL
        );

        ScrollView scrollDaftar =
                new ScrollView(this);

        scrollDaftar.addView(
                daftar
        );

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        layout.addView(
                scrollDaftar,
                scrollParams
        );

        Button kembali =
                new Button(this);

        kembali.setText(
                "🔙 KEMBALI KE MENU UTAMA"
        );

        layout.addView(
                kembali
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "📋 RIWAYAT NOTA"
                        )
                        .setView(layout)
                        .create();

        // =====================================================
        // TAMPILKAN DATA
        // =====================================================

        Runnable tampilkanSemua =
                () -> {

                    daftar.removeAllViews();

                    hasilCari.clear();
                    timestampCari.clear();

                    SharedPreferences pref =
                            getSharedPreferences(
                                    PREF_NAME,
                                    MODE_PRIVATE
                            );

                    String data =
                            pref.getString(
                                    KEY_HISTORY,
                                    ""
                            );

                    if (data.isEmpty()) {

                        TextView kosong =
                                new TextView(this);

                        kosong.setText(
                                "Belum ada nota tersimpan."
                        );

                        kosong.setTextSize(16);

                        kosong.setPadding(
                                10,
                                20,
                                10,
                                20
                        );

                        daftar.addView(
                                kosong
                        );

                        return;
                    }

                    String kata =
                            cariInput.getText()
                                    .toString()
                                    .trim()
                                    .toLowerCase(
                                            Locale.getDefault()
                                    );

                    String[] semua =
                            data.split("\n");

                    for (int i =
                         semua.length - 1;
                         i >= 0;
                         i--) {

                        String nota =
                                semua[i];

                        try {

                            String[] bagian =
                                    nota.split(
                                            "\\|",
                                            8
                                    );

                            if (bagian.length < 7) {
                                continue;
                            }

                            long timestamp =
                                    Long.parseLong(
                                            bagian[0]
                                    );

                            String nama =
                                    decode(
                                            bagian[1]
                                    );

                            String wa =
                                    decode(
                                            bagian[2]
                                    );

                            String tanggal =
                                    decode(
                                            bagian[3]
                                    );

                            String motor =
                                    decode(
                                            bagian[4]
                                    );

                            long dp =
                                    Long.parseLong(
                                            bagian[5]
                                    );

                            String status;

                            String itemData;

                            /*
                             * Format baru:
                             * timestamp|nama|wa|tanggal|motor|dp|status|items
                             *
                             * Format lama:
                             * timestamp|nama|wa|tanggal|motor|dp|items
                             */
                            if (bagian.length >= 8) {

                                status =
                                        decode(
                                                bagian[6]
                                        );

                                itemData =
                                        bagian[7];

                            } else {

                                itemData =
                                        bagian[6];

                                long totalLama =
                                        hitungTotalItem(
                                                itemData
                                        );

                                status =
                                        dp >= totalLama
                                                ? "LUNAS"
                                                : "BELUM LUNAS";
                            }

                            if (!kata.isEmpty()) {

                                boolean cocokNama =
                                        nama.toLowerCase(
                                                Locale.getDefault()
                                        ).contains(
                                                kata
                                        );

                                boolean cocokWa =
                                        wa.toLowerCase(
                                                Locale.getDefault()
                                        ).contains(
                                                kata
                                        );

                                if (!cocokNama &&
                                        !cocokWa) {

                                    continue;
                                }
                            }

                            long total =
                                    hitungTotalItem(
                                            itemData
                                    );

                            long sisa =
                                    total - dp;

                            if (sisa < 0) {
                                sisa = 0;
                            }

                            hasilCari.add(
                                    nota
                            );

                            timestampCari.add(
                                    timestamp
                            );

                            daftar.addView(
                                    buatKartuRiwayat(
                                            nama,
                                            wa,
                                            tanggal,
                                            motor,
                                            total,
                                            dp,
                                            sisa,
                                            status,
                                            timestamp,
                                            dialog
                                    )
                            );

                        } catch (Exception ignored) {
                        }
                    }

                    if (daftar.getChildCount() == 0) {

                        TextView kosong =
                                new TextView(this);

                        kosong.setText(
                                "Riwayat tidak ditemukan."
                        );

                        kosong.setTextSize(16);

                        kosong.setPadding(
                                10,
                                20,
                                10,
                                20
                        );

                        daftar.addView(
                                kosong
                        );
                    }
                };

        tombolCari.setOnClickListener(
                v -> tampilkanSemua.run()
        );

        cariInput.setOnEditorActionListener(
                (v, actionId, event) -> {

                    tampilkanSemua.run();

                    return true;
                }
        );

        kembali.setOnClickListener(
                v -> dialog.dismiss()
        );

        tampilkanSemua.run();

        dialog.show();

        WindowManager.LayoutParams lp =
                new WindowManager.LayoutParams();

        if (dialog.getWindow() != null) {

            lp.copyFrom(
                    dialog.getWindow()
                            .getAttributes()
            );

            lp.width =
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .widthPixels
                                    * 0.96
                    );

            lp.height =
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .heightPixels
                                    * 0.88
                    );

            dialog.getWindow()
                    .setAttributes(lp);
        }
    }

    // =========================================================
    // KARTU RIWAYAT
    // =========================================================

    private LinearLayout buatKartuRiwayat(
            String nama,
            String wa,
            String tanggal,
            String motor,
            long total,
            long dp,
            long sisa,
            String status,
            long timestamp,
            AlertDialog dialog) {

        LinearLayout kartu =
                new LinearLayout(this);

        kartu.setOrientation(
                LinearLayout.VERTICAL
        );

        kartu.setPadding(
                15,
                12,
                15,
                12
        );

        TextView info =
                new TextView(this);

        StringBuilder teks =
                new StringBuilder();

        teks.append(
                "👤 "
        )
        .append(nama)
        .append("\n");

        teks.append(
                "📱 "
        )
        .append(wa)
        .append("\n");

        teks.append(
                "📅 "
        )
        .append(tanggal)
        .append("\n");

        if (!motor.isEmpty()) {

            teks.append(
                    "🏍️ "
            )
            .append(motor)
            .append("\n");
        }

        teks.append(
                "💰 TOTAL : "
        )
        .append(
                formatRupiah(total)
        )
        .append("\n");

        teks.append(
                "💵 DP : "
        )
        .append(
                formatRupiah(dp)
        )
        .append("\n");

        teks.append(
                "💳 SISA : "
        )
        .append(
                formatRupiah(sisa)
        )
        .append("\n");

        if ("LUNAS".equals(status)) {

            teks.append(
                    "✅ STATUS : LUNAS"
            );

        } else {

            teks.append(
                    "⚠️ STATUS : BELUM LUNAS"
            );
        }

        info.setText(
                teks.toString()
        );

        info.setTextSize(15);

        kartu.addView(
                info
        );

        // =====================================================
        // TOMBOL EDIT
        // =====================================================

        Button edit =
                new Button(this);

        edit.setText(
                "✏️ EDIT NOTA"
        );

        edit.setTextSize(12);

        kartu.addView(
                edit
        );

        edit.setOnClickListener(
                v -> {

                    dialog.dismiss();

                    bukaNotaUntukEdit(
                            timestamp
                    );
                }
        );

        // =====================================================
        // TOMBOL PRINT
        // =====================================================

        Button print =
                new Button(this);

        print.setText(
                "🖨️ PRINT BLUETOOTH"
        );

        print.setTextSize(12);

        kartu.addView(
                print
        );

        print.setOnClickListener(
                v -> {

                    muatNotaDariRiwayat(
                            timestamp
                    );

                    mulaiCetakBluetooth();
                }
        );

        // =====================================================
        // TOMBOL WHATSAPP
        // =====================================================

        Button waButton =
                new Button(this);

        waButton.setText(
                "💬 KIRIM WHATSAPP"
        );

        waButton.setTextSize(12);

        kartu.addView(
                waButton
        );

        waButton.setOnClickListener(
                v -> {

                    muatNotaDariRiwayat(
                            timestamp
                    );

                    kirimWhatsApp();
                }
        );

        // =====================================================
        // TOMBOL HAPUS
        // =====================================================

        Button hapus =
                new Button(this);

        hapus.setText(
                "🗑️ HAPUS RIWAYAT"
        );

        hapus.setTextSize(12);

        kartu.addView(
                hapus
        );

        hapus.setOnClickListener(
                v -> {

                    new AlertDialog.Builder(
                            this
                    )
                            .setTitle(
                                    "Hapus nota?"
                            )
                            .setMessage(
                                    "Nota " +
                                            nama +
                                            " akan dihapus dari riwayat."
                            )
                            .setNegativeButton(
                                    "BATAL",
                                    null
                            )
                            .setPositiveButton(
                                    "HAPUS",
                                    (d, which) -> {

                                        hapusRiwayat(
                                                timestamp
                                        );

                                        dialog.dismiss();

                                        tampilkanRiwayat();
                                    }
                            )
                            .show();
                }
        );

        TextView garis =
                new TextView(this);

        garis.setText(
                "────────────────────"
        );

        garis.setGravity(
                Gravity.CENTER
        );

        kartu.addView(
                garis
        );

        return kartu;
    }

    // =========================================================
    // HITUNG TOTAL ITEM DARI DATA
    // =========================================================

    private long hitungTotalItem(
            String itemData) {

        long total = 0;

        if (itemData == null ||
                itemData.isEmpty()) {

            return 0;
        }

        String[] items =
                itemData.split(";");

        for (String item :
                items) {

            try {

                String[] isi =
                        item.split(
                                "~"
                        );

                if (isi.length >= 3) {

                    long jumlah =
                            Long.parseLong(
                                    isi[1]
                            );

                    long harga =
                            Long.parseLong(
                                    isi[2]
                            );

                    total +=
                            jumlah * harga;
                }

            } catch (Exception ignored) {
            }
        }

        return total;
    }

    // =========================================================
    // HAPUS RIWAYAT
    // =========================================================

    private void hapusRiwayat(
            long timestamp) {

        SharedPreferences pref =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String data =
                pref.getString(
                        KEY_HISTORY,
                        ""
                );

        if (data.isEmpty()) {
            return;
        }

        String[] semua =
                data.split("\n");

        StringBuilder hasil =
                new StringBuilder();

        for (String nota :
                semua) {

            try {

                String[] bagian =
                        nota.split(
                                "\\|",
                                8
                        );

                long waktu =
                        Long.parseLong(
                                bagian[0]
                        );

                if (waktu ==
                        timestamp) {

                    continue;
                }

                if (hasil.length() > 0) {
                    hasil.append("\n");
                }

                hasil.append(nota);

            } catch (Exception e) {

                if (hasil.length() > 0) {
                    hasil.append("\n");
                }

                hasil.append(nota);
            }
        }

        pref.edit()
                .putString(
                        KEY_HISTORY,
                        hasil.toString()
                )
                .apply();

        Toast.makeText(
                this,
                "Riwayat berhasil dihapus 🗑️",
                Toast.LENGTH_LONG
        ).show();
    }

    // =========================================================
    // BUKA NOTA UNTUK EDIT
    // =========================================================

    private void bukaNotaUntukEdit(
            long timestamp) {

        String data =
                ambilNotaDariRiwayat(
                        timestamp
                );

        if (data == null) {

            Toast.makeText(
                    this,
                    "Nota tidak ditemukan",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        tampilkanMenuUtama();

        if (!muatDataNota(
                data
        )) {

            Toast.makeText(
                    this,
                    "Gagal membaca nota",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        editingTimestamp =
                timestamp;

        Toast.makeText(
                this,
                "Mode EDIT nota aktif ✏️",
                Toast.LENGTH_LONG
        ).show();
    }

    // =========================================================
    // AMBIL NOTA
    // =========================================================

    private String ambilNotaDariRiwayat(
            long timestamp) {

        SharedPreferences pref =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String data =
                pref.getString(
                        KEY_HISTORY,
                        ""
                );

        if (data.isEmpty()) {
            return null;
        }

        String[] semua =
                data.split("\n");

        for (String nota :
                semua) {

            try {

                String[] bagian =
                        nota.split(
                                "\\|",
                                8
                        );

                if (bagian.length >= 7) {

                    long waktu =
                            Long.parseLong(
                                    bagian[0]
                            );

                    if (waktu ==
                            timestamp) {

                        return nota;
                    }
                }

            } catch (Exception ignored) {
            }
        }

        return null;
    }

    // =========================================================
    // MUAT NOTA DARI RIWAYAT
    // =========================================================

    private boolean muatNotaDariRiwayat(
            long timestamp) {

        String data =
                ambilNotaDariRiwayat(
                        timestamp
                );

        if (data == null) {
            return false;
        }

        return muatDataNota(
                data
        );
    }

    // =========================================================
    // MUAT DATA NOTA KE HALAMAN UTAMA
    // =========================================================

    private boolean muatDataNota(
            String data) {

        try {

            String[] bagian =
                    data.split(
                            "\\|",
                            8
                    );

            if (bagian.length < 7) {
                return false;
            }

            namaInput.setText(
                    decode(
                            bagian[1]
                    )
            );

            waInput.setText(
                    decode(
                            bagian[2]
                    )
            );

            tanggalInput.setText(
                    decode(
                            bagian[3]
                    )
            );

            motorInput.setText(
                    decode(
                            bagian[4]
                    )
            );

            dpInput.setText(
                    bagian[5]
            );

            String itemData;

            if (bagian.length >= 8) {

                statusBayar =
                        decode(
                                bagian[6]
                        );

                itemData =
                        bagian[7];

            } else {

                itemData =
                        bagian[6];

                long total =
                        hitungTotalItem(
                                itemData
                        );

                long dp =
                        Long.parseLong(
                                bagian[5]
                        );

                statusBayar =
                        dp >= total
                                ? "LUNAS"
                                : "BELUM LUNAS";
            }

            if (!"LUNAS".equals(
                    statusBayar
            )) {

                statusBayar =
                        "BELUM LUNAS";
            }

            if (statusSpinner != null) {

                statusSpinner.setSelection(
                        "LUNAS".equals(
                                statusBayar
                        )
                                ? 1
                                : 0
                );
            }

            // =================================================
            // BERSIHKAN ITEM LAMA
            // =================================================

            itemContainer.removeAllViews();

            namaBarang.clear();
            jumlahBarang.clear();
            hargaBarang.clear();

            String[] items =
                    itemData.split(";");

            boolean adaItem = false;

            for (String item :
                    items) {

                String[] isi =
                        item.split(
                                "~"
                        );

                if (isi.length < 3) {
                    continue;
                }

                String nama =
                        decode(
                                isi[0]
                        );

                long jumlah =
                        Long.parseLong(
                                isi[1]
                        );

                long harga =
                        Long.parseLong(
                                isi[2]
                        );

                tambahBarisItem();

                int posisi =
                        namaBarang.size() - 1;

                namaBarang.get(
                        posisi
                ).setText(
                        nama
                );

                jumlahBarang.get(
                        posisi
                ).setText(
                        String.valueOf(
                                jumlah
                        )
                );

                hargaBarang.get(
                        posisi
                ).setText(
                        String.valueOf(
                                harga
                        )
                );

                adaItem = true;
            }

            if (!adaItem) {

                tambahBarisItem();
            }

            hitungTotal();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // TEKS NOTA CETAK
    // FORMAT TIDAK DIUBAH
    // =========================================================

    private String buatTeksNota() {

        String nama =
                namaInput.getText()
                        .toString()
                        .trim();

        String wa =
                waInput.getText()
                        .toString()
                        .trim();

        String tanggal =
                tanggalInput.getText()
                        .toString()
                        .trim();

        String motor =
                motorInput.getText()
                        .toString()
                        .trim();

        long total =
                hitungTotal();

        long dp =
                angka(dpInput);

        long sisa =
                total - dp;

        if (sisa < 0) {
            sisa = 0;
        }

        StringBuilder nota =
                new StringBuilder();

        nota.append(
                "================================\n"
        );

        nota.append(
                "            RR MOTOR\n"
        );

        nota.append(
                "          NOTA SERVIS\n"
        );

        nota.append(
                "================================\n\n"
        );

        nota.append(
                "Nama   : "
        )
        .append(nama)
        .append("\n");

        nota.append(
                "WA     : "
        )
        .append(wa)
        .append("\n");

        nota.append(
                "Tanggal: "
        )
        .append(tanggal)
        .append("\n");

        if (!motor.isEmpty()) {

            nota.append(
                    "Motor  : "
            )
            .append(motor)
            .append("\n");
        }

        nota.append("\n");

        nota.append(
                "--------------------------------\n"
        );

        nota.append(
                "BARANG / JASA\n"
        );

        nota.append(
                "--------------------------------\n"
        );

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            String barang =
                    namaBarang.get(i)
                            .getText()
                            .toString()
                            .trim();

            long jumlah =
                    angka(
                            jumlahBarang.get(i)
                    );

            long harga =
                    angka(
                            hargaBarang.get(i)
                    );

            if (!barang.isEmpty()) {

                nota.append(
                        barang
                )
                .append("\n");

                nota.append(
                        "  "
                )
                .append(jumlah)
                .append(" x ")
                .append(
                        formatRupiah(
                                harga
                        )
                )
                .append("\n");

                nota.append(
                        "  SUBTOTAL: "
                )
                .append(
                        formatRupiah(
                                jumlah * harga
                        )
                )
                .append("\n\n");
            }
        }

        nota.append(
                "--------------------------------\n"
        );

        nota.append(
                "TOTAL : "
        )
        .append(
                formatRupiah(total)
        )
        .append("\n");

        nota.append(
                "DP    : "
        )
        .append(
                formatRupiah(dp)
        )
        .append("\n");

        nota.append(
                "SISA  : "
        )
        .append(
                formatRupiah(sisa)
        )
        .append("\n");

        /*
         * Bentuk tulisan cetak tetap:
         * STATUS: LUNAS
         * STATUS: BELUM LUNAS
         *
         * Yang berubah hanya pilihan statusnya.
         */

        if ("LUNAS".equals(
                statusBayar
        )) {

            nota.append(
                    "STATUS: LUNAS\n"
            );

        } else {

            nota.append(
                    "STATUS: BELUM LUNAS\n"
            );
        }

        nota.append("\n");

        nota.append(
                "--------------------------------\n"
        );

        nota.append(
                "Terima kasih.\n"
        );

        nota.append(
                "RR MOTOR\n"
        );

        nota.append(
                "================================\n"
        );

        return nota.toString();
    }

    // =========================================================
    // CETAK ANDROID / PDF
    // =========================================================

    private void cetakNota() {

        if (!validasiNota()) {
            return;
        }

        try {

            PrintManager printManager =
                    (PrintManager)
                            getSystemService(
                                    Context.PRINT_SERVICE
                            );

            if (printManager == null) {

                Toast.makeText(
                        this,
                        "Layanan cetak tidak tersedia",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            String isiNota =
                    buatTeksNota();

            NotaPrintAdapter adapter =
                    new NotaPrintAdapter(
                            isiNota
                    );

            printManager.print(
                    "RR MOTOR NOTA",
                    adapter,
                    new PrintAttributes.Builder()
                            .setMediaSize(
                                    PrintAttributes.MediaSize.ISO_A4
                            )
                            .setMinMargins(
                                    PrintAttributes.Margins.NO_MARGINS
                            )
                            .build()
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Gagal membuka menu cetak: " +
                            e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // VALIDASI
    // =========================================================

    private boolean validasiNota() {

        String nama =
                namaInput.getText()
                        .toString()
                        .trim();

        String wa =
                waInput.getText()
                        .toString()
                        .trim();

        String tanggal =
                tanggalInput.getText()
                        .toString()
                        .trim();

        if (nama.isEmpty()) {

            namaInput.setError(
                    "Nama pelanggan wajib diisi"
            );

            namaInput.requestFocus();

            tampilkanKeyboard(
                    namaInput
            );

            return false;
        }

        if (wa.isEmpty()) {

            waInput.setError(
                    "Nomor WhatsApp wajib diisi"
            );

            waInput.requestFocus();

            tampilkanKeyboard(
                    waInput
            );

            return false;
        }

        if (tanggal.isEmpty()) {

            Toast.makeText(
                    this,
                    "Tanggal nota wajib diisi",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        boolean adaBarang = false;

        for (EditText item :
                namaBarang) {

            if (!item.getText()
                    .toString()
                    .trim()
                    .isEmpty()) {

                adaBarang = true;

                break;
            }
        }

        if (!adaBarang) {

            Toast.makeText(
                    this,
                    "Masukkan minimal 1 barang atau jasa",
                    Toast.LENGTH_LONG
            ).show();

            if (!namaBarang.isEmpty()) {

                namaBarang.get(0)
                        .requestFocus();

                tampilkanKeyboard(
                        namaBarang.get(0)
                );
            }

            return false;
        }

        return true;
    }

    // =========================================================
    // WHATSAPP
    // =========================================================

    private void kirimWhatsApp() {

        if (!validasiNota()) {
            return;
        }

        String nama =
                namaInput.getText()
                        .toString()
                        .trim();

        String wa =
                waInput.getText()
                        .toString()
                        .trim();

        long total =
                hitungTotal();

        long dp =
                angka(dpInput);

        long sisa =
                total - dp;

        if (sisa < 0) {
            sisa = 0;
        }

        StringBuilder pesan =
                new StringBuilder();

        pesan.append(
                "🏍️ *RR MOTOR*\n\n"
        );

        pesan.append(
                "Halo Bapak/Ibu *"
        )
        .append(nama)
        .append("* 👋\n\n");

        pesan.append(
                "Berikut informasi nota servis Anda:\n\n"
        );

        String motor =
                motorInput.getText()
                        .toString()
                        .trim();

        if (!motor.isEmpty()) {

            pesan.append(
                    "🏍️ Motor: "
            )
            .append(motor)
            .append("\n\n");
        }

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            String barang =
                    namaBarang.get(i)
                            .getText()
                            .toString()
                            .trim();

            long jumlah =
                    angka(
                            jumlahBarang.get(i)
                    );

            long harga =
                    angka(
                            hargaBarang.get(i)
                    );

            if (!barang.isEmpty()) {

                pesan.append(
                        "🔧 "
                )
                .append(barang)
                .append("\n");

                pesan.append(
                        "   "
                )
                .append(jumlah)
                .append(" x ")
                .append(
                        formatRupiah(
                                harga
                        )
                )
                .append("\n");

                pesan.append(
                        "   = "
                )
                .append(
                        formatRupiah(
                                jumlah * harga
                        )
                )
                .append("\n\n");
            }
        }

        pesan.append(
                "*TOTAL : "
        )
        .append(
                formatRupiah(total)
        )
        .append("*\n");

        pesan.append(
                "DP : "
        )
        .append(
                formatRupiah(dp)
        )
        .append("\n");

        pesan.append(
                "SISA : "
        )
        .append(
                formatRupiah(sisa)
        )
        .append("\n\n");

        if ("LUNAS".equals(
                statusBayar
        )) {

            pesan.append(
                    "✅ *STATUS: LUNAS*\n\n"
            );

        } else {

            pesan.append(
                    "⚠️ *STATUS: BELUM LUNAS*\n\n"
            );
        }

        pesan.append(
                "Terima kasih sudah mempercayakan " +
                        "kendaraan Anda kepada " +
                        "*RR MOTOR*. 🙏"
        );

        String nomor =
                wa.replaceAll(
                        "[^0-9]",
                        ""
                );

        if (nomor.startsWith("0")) {

            nomor =
                    "62" +
                            nomor.substring(1);
        }

        if (nomor.isEmpty()) {

            Toast.makeText(
                    this,
                    "Nomor WhatsApp tidak valid",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            String url =
                    "https://wa.me/" +
                            nomor +
                            "?text=" +
                            Uri.encode(
                                    pesan.toString()
                            );

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            startActivity(
                    intent
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "WhatsApp tidak dapat dibuka",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // BLUETOOTH
    // =========================================================

    private void mulaiCetakBluetooth() {

        if (!validasiNota()) {
            return;
        }

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

            if (checkSelfPermission(
                    Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        REQUEST_BLUETOOTH
                );

                return;
            }
        }

        BluetoothManager bluetoothManager =
                (BluetoothManager)
                        getSystemService(
                                Context.BLUETOOTH_SERVICE
                        );

        if (bluetoothManager == null) {

            Toast.makeText(
                    this,
                    "Bluetooth tidak tersedia",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        BluetoothAdapter adapter =
                bluetoothManager.getAdapter();

        if (adapter == null) {

            Toast.makeText(
                    this,
                    "HP tidak memiliki Bluetooth",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (!adapter.isEnabled()) {

            try {

                Intent intent =
                        new Intent(
                                BluetoothAdapter.ACTION_REQUEST_ENABLE
                        );

                startActivity(
                        intent
                );

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Silakan aktifkan Bluetooth HP",
                        Toast.LENGTH_LONG
                ).show();
            }

            return;
        }

        tampilkanPrinterBluetooth(
                adapter
        );
    }

    // =========================================================
    // PILIH PRINTER
    // =========================================================

    private void tampilkanPrinterBluetooth(
            BluetoothAdapter adapter) {

        try {

            Set<BluetoothDevice> bondedDevices =
                    adapter.getBondedDevices();

            if (bondedDevices == null ||
                    bondedDevices.isEmpty()) {

                new AlertDialog.Builder(this)
                        .setTitle(
                                "🔵 PRINTER BLUETOOTH"
                        )
                        .setMessage(
                                "Belum ada perangkat Bluetooth " +
                                        "yang dipasangkan.\n\n" +
                                        "Pasangkan printer terlebih dahulu " +
                                        "melalui Pengaturan Bluetooth HP."
                        )
                        .setPositiveButton(
                                "OK",
                                null
                        )
                        .show();

                return;
            }

            ArrayList<BluetoothDevice> daftar =
                    new ArrayList<>();

            ArrayList<String> nama =
                    new ArrayList<>();

            for (BluetoothDevice device :
                    bondedDevices) {

                daftar.add(
                        device
                );

                String namaPrinter;

                try {

                    namaPrinter =
                            device.getName();

                } catch (SecurityException e) {

                    namaPrinter =
                            "Printer Bluetooth";
                }

                if (namaPrinter == null ||
                        namaPrinter.trim().isEmpty()) {

                    namaPrinter =
                            "Printer Bluetooth";
                }

                String alamat =
                        device.getAddress();

                nama.add(
                        namaPrinter +
                                "\n" +
                                alamat
                );
            }

            String[] pilihan =
                    nama.toArray(
                            new String[0]
                    );

            new AlertDialog.Builder(this)
                    .setTitle(
                            "🔵 PILIH PRINTER"
                    )
                    .setItems(
                            pilihan,
                            (dialog, which) -> {

                                BluetoothDevice device =
                                        daftar.get(
                                                which
                                        );

                                cetakKePrinter(
                                        device
                                );
                            }
                    )
                    .setNegativeButton(
                            "BATAL",
                            null
                    )
                    .show();

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "Izin Bluetooth belum diberikan",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // CETAK KE PRINTER
    // =========================================================

    private void cetakKePrinter(
            BluetoothDevice device) {

        String isiNota =
                buatTeksNota();

        Toast.makeText(
                this,
                "Menghubungkan ke printer...",
                Toast.LENGTH_SHORT
        ).show();

        new Thread(
                () -> {

                    try {

                        BluetoothPrinter printer =
                                new BluetoothPrinter(
                                        device
                                );

                        printer.connect();

                        printer.print(
                                isiNota
                        );

                        printer.disconnect();

                        runOnUiThread(
                                () -> {

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Nota berhasil dicetak ✅",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                        );

                    } catch (Exception e) {

                        runOnUiThread(
                                () -> {

                                    Toast.makeText(
                                            MainActivity.this,
                                            "Gagal mencetak Bluetooth: " +
                                                    e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                        );
                    }

                }
        ).start();
    }

    // =========================================================
    // IZIN BLUETOOTH
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                REQUEST_BLUETOOTH) {

            boolean semuaDiizinkan =
                    true;

            for (int hasil :
                    grantResults) {

                if (hasil !=
                        PackageManager.PERMISSION_GRANTED) {

                    semuaDiizinkan =
                            false;

                    break;
                }
            }

            if (semuaDiizinkan) {

                mulaiCetakBluetooth();

            } else {

                Toast.makeText(
                        this,
                        "Izin Bluetooth diperlukan " +
                                "untuk mencetak nota",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // =========================================================
    // KEYBOARD
    // =========================================================

    private void tampilkanKeyboard(
            EditText input) {

        input.postDelayed(
                () -> {

                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager)
                                    getSystemService(
                                            Context.INPUT_METHOD_SERVICE
                                    );

                    if (imm != null) {

                        imm.showSoftInput(
                                input,
                                android.view.inputmethod.InputMethodManager
                                        .SHOW_IMPLICIT
                        );
                    }

                    if (scrollView != null) {

                        scrollView.postDelayed(
                                () -> {

                                    scrollView.smoothScrollTo(
                                            0,
                                            input.getBottom()
                                    );

                                },
                                200
                        );
                    }

                },
                150
        );
    }
}
