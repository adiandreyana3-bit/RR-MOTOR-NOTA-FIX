package com.rrmotor.nota;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText namaInput;
    private EditText waInput;
    private EditText tanggalInput;
    private EditText motorInput;
    private EditText dpInput;

    private LinearLayout itemContainer;

    private TextView totalText;
    private TextView sisaText;
    private TextView statusText;

    private final ArrayList<EditText> namaBarang = new ArrayList<>();
    private final ArrayList<EditText> jumlahBarang = new ArrayList<>();
    private final ArrayList<EditText> hargaBarang = new ArrayList<>();

    private static final String PREF_NAME = "RR_MOTOR_NOTA";
    private static final String KEY_HISTORY = "HISTORY";

    // 1 tahun dalam milidetik
    private static final long SATU_TAHUN =
            365L * 24L * 60L * 60L * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bersihkan riwayat yang sudah lebih dari 1 tahun
        bersihkanRiwayatLama();

        ScrollView scrollView = new ScrollView(this);

        LinearLayout utama = new LinearLayout(this);
        utama.setOrientation(LinearLayout.VERTICAL);
        utama.setPadding(25, 25, 25, 40);

        scrollView.addView(utama);

        // =========================
        // JUDUL
        // =========================

        TextView judul = new TextView(this);
        judul.setText("🏍️ RR MOTOR NOTA");
        judul.setTextSize(28);
        judul.setTypeface(null, Typeface.BOLD);
        judul.setGravity(Gravity.CENTER);
        judul.setPadding(0, 10, 0, 25);

        utama.addView(judul);

        // =========================
        // DATA PELANGGAN
        // =========================

        namaInput = buatInput("Nama pelanggan *");
        utama.addView(namaInput);

        waInput = buatInput("Nomor WhatsApp *");
        waInput.setInputType(InputType.TYPE_CLASS_PHONE);
        utama.addView(waInput);

        tanggalInput = buatInput("Tanggal nota *");
        tanggalInput.setFocusable(false);
        tanggalInput.setClickable(true);
        tanggalInput.setOnClickListener(v -> pilihTanggal());

        utama.addView(tanggalInput);

        motorInput = buatInput("Jenis motor (opsional)");
        utama.addView(motorInput);

        dpInput = buatInput("DP / Uang muka (opsional)");
        dpInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        utama.addView(dpInput);

        // =========================
        // JUDUL ITEM
        // =========================

        TextView judulItem = new TextView(this);
        judulItem.setText("🧾 DAFTAR BARANG / JASA");
        judulItem.setTextSize(20);
        judulItem.setTypeface(null, Typeface.BOLD);
        judulItem.setPadding(0, 25, 0, 10);

        utama.addView(judulItem);

        // =========================
        // CONTAINER ITEM
        // =========================

        itemContainer = new LinearLayout(this);
        itemContainer.setOrientation(LinearLayout.VERTICAL);

        utama.addView(itemContainer);

        // =========================
        // TOMBOL TAMBAH ITEM
        // =========================

        Button tambahItem = new Button(this);
        tambahItem.setText("＋ Tambah Barang / Jasa");
        tambahItem.setOnClickListener(v -> tambahBarisItem());

        utama.addView(tambahItem);

        // =========================
        // TOTAL
        // =========================

        totalText = new TextView(this);
        totalText.setTextSize(20);
        totalText.setTypeface(null, Typeface.BOLD);
        totalText.setPadding(0, 25, 0, 5);

        utama.addView(totalText);

        // =========================
        // SISA
        // =========================

        sisaText = new TextView(this);
        sisaText.setTextSize(20);
        sisaText.setTypeface(null, Typeface.BOLD);

        utama.addView(sisaText);

        // =========================
        // STATUS
        // =========================

        statusText = new TextView(this);
        statusText.setTextSize(18);
        statusText.setPadding(0, 5, 0, 15);

        utama.addView(statusText);

        // =========================
        // TOMBOL SIMPAN
        // =========================

        Button simpan = new Button(this);
        simpan.setText("💾 SIMPAN NOTA");
        simpan.setOnClickListener(v -> simpanNota());

        utama.addView(simpan);

        // =========================
        // TOMBOL CETAK
        // =========================

        Button cetak = new Button(this);
        cetak.setText("🖨️ CETAK NOTA");
        cetak.setOnClickListener(v -> cetakNota());

        utama.addView(cetak);

        // =========================
        // TOMBOL RIWAYAT
        // =========================

        Button riwayat = new Button(this);
        riwayat.setText("📋 RIWAYAT NOTA");
        riwayat.setOnClickListener(v -> tampilkanRiwayat());

        utama.addView(riwayat);

        // =========================
        // TOMBOL WHATSAPP
        // =========================

        Button whatsapp = new Button(this);
        whatsapp.setText("💬 KIRIM VIA WHATSAPP");
        whatsapp.setOnClickListener(v -> kirimWhatsApp());

        utama.addView(whatsapp);

        setContentView(scrollView);

        // =========================
        // TANGGAL OTOMATIS
        // =========================

        tanggalInput.setText(
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                ).format(new Date())
        );

        // =========================
        // ITEM PERTAMA
        // =========================

        tambahBarisItem();

        // =========================
        // HITUNG AWAL
        // =========================

        pasangListenerHitung(dpInput);

        hitungTotal();
    }

    // =========================================================
    // MEMBUAT INPUT
    // =========================================================

    private EditText buatInput(String hint) {

        EditText input = new EditText(this);

        input.setHint(hint);
        input.setTextSize(17);
        input.setSingleLine(true);
        input.setPadding(20, 10, 20, 10);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 5, 0, 5);

        input.setLayoutParams(params);

        return input;
    }

    // =========================================================
    // LISTENER HITUNG
    // =========================================================

    private void pasangListenerHitung(EditText input) {

        input.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                hitungTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // =========================================================
    // PILIH TANGGAL
    // =========================================================

    private void pilihTanggal() {

        Calendar kalender = Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, day) -> {

                            String tanggal =
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d/%02d/%04d",
                                            day,
                                            month + 1,
                                            year
                                    );

                            tanggalInput.setText(tanggal);
                        },
                        kalender.get(Calendar.YEAR),
                        kalender.get(Calendar.MONTH),
                        kalender.get(Calendar.DAY_OF_MONTH)
                );

        dialog.show();
    }

    // =========================================================
    // TAMBAH BARIS ITEM
    // =========================================================

    private void tambahBarisItem() {

        LinearLayout baris =
                new LinearLayout(this);

        baris.setOrientation(
                LinearLayout.VERTICAL
        );

        baris.setPadding(0, 10, 0, 10);

        // Nama barang
        EditText nama =
                buatInput("Nama barang / jasa");

        // Jumlah
        EditText jumlah =
                buatInput("Jumlah");

        // Harga
        EditText harga =
                buatInput("Harga satuan");

        jumlah.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        harga.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        // Tombol hapus
        Button hapus =
                new Button(this);

        hapus.setText("🗑️ Hapus item");

        baris.addView(nama);
        baris.addView(jumlah);
        baris.addView(harga);
        baris.addView(hapus);

        itemContainer.addView(baris);

        namaBarang.add(nama);
        jumlahBarang.add(jumlah);
        hargaBarang.add(harga);

        // Hitung otomatis saat diketik
        pasangListenerHitung(jumlah);
        pasangListenerHitung(harga);

        // Tombol hapus
        hapus.setOnClickListener(v -> {

            int posisi =
                    namaBarang.indexOf(nama);

            if (namaBarang.size() > 1) {

                if (posisi >= 0) {

                    namaBarang.remove(posisi);
                    jumlahBarang.remove(posisi);
                    hargaBarang.remove(posisi);

                    itemContainer.removeView(baris);

                    hitungTotal();
                }

            } else {

                Toast.makeText(
                        this,
                        "Minimal harus ada 1 item",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // =========================================================
    // ANGKA
    // =========================================================

    private long angka(EditText input) {

        if (input == null) {
            return 0;
        }

        try {

            String teks =
                    input.getText()
                            .toString()
                            .replace(".", "")
                            .replace(",", "")
                            .trim();

            if (teks.isEmpty()) {
                return 0;
            }

            return Long.parseLong(teks);

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // HITUNG TOTAL
    // =========================================================

    private long hitungTotal() {

        long total = 0;

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            long jumlah =
                    angka(jumlahBarang.get(i));

            long harga =
                    angka(hargaBarang.get(i));

            // Hindari perhitungan aneh
            if (jumlah < 0) {
                jumlah = 0;
            }

            if (harga < 0) {
                harga = 0;
            }

            total += jumlah * harga;
        }

        long dp = angka(dpInput);

        if (dp < 0) {
            dp = 0;
        }

        long sisa = total - dp;

        if (sisa < 0) {
            sisa = 0;
        }

        totalText.setText(
                "TOTAL : " +
                        formatRupiah(total)
        );

        sisaText.setText(
                "SISA : " +
                        formatRupiah(sisa)
        );

        if (sisa == 0) {

            statusText.setText(
                    "STATUS : LUNAS ✅"
            );

        } else {

            statusText.setText(
                    "STATUS : BELUM LUNAS ⚠️"
            );
        }

        return total;
    }

    // =========================================================
    // FORMAT RUPIAH
    // =========================================================

    private String formatRupiah(long angka) {

        NumberFormat format =
                NumberFormat.getNumberInstance(
                        new Locale("id", "ID")
                );

        return "Rp " + format.format(angka);
    }

    // =========================================================
    // SIMPAN NOTA
    // =========================================================

    private void simpanNota() {

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

        // Nama wajib
        if (nama.isEmpty()) {

            namaInput.setError(
                    "Nama wajib diisi"
            );

            namaInput.requestFocus();

            return;
        }

        // WA wajib
        if (wa.isEmpty()) {

            waInput.setError(
                    "Nomor WhatsApp wajib diisi"
            );

            waInput.requestFocus();

            return;
        }

        // Tanggal wajib
        if (tanggal.isEmpty()) {

            tanggalInput.setError(
                    "Tanggal wajib diisi"
            );

            return;
        }

        long total = hitungTotal();

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
                    angka(jumlahBarang.get(i));

            long harga =
                    angka(hargaBarang.get(i));

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

        // Harus ada minimal barang/jasa
        if (dataItem.length() == 0) {

            Toast.makeText(
                    this,
                    "Masukkan minimal 1 barang atau jasa",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        long waktu =
                System.currentTimeMillis();

        String data =
                waktu + "|" +
                encode(nama) + "|" +
                encode(wa) + "|" +
                encode(tanggal) + "|" +
                encode(
                        motorInput.getText()
                                .toString()
                                .trim()
                ) + "|" +
                angka(dpInput) + "|" +
                dataItem;

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

    // =========================================================
    // ENCODE
    // =========================================================

    private String encode(String teks) {

        if (teks == null) {
            return "";
        }

        return teks
                .replace("|", "%7C")
                .replace(";", "%3B")
                .replace("~", "%7E")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    // =========================================================
    // DECODE
    // =========================================================

    private String decode(String teks) {

        if (teks == null) {
            return "";
        }

        return teks
                .replace("%7C", "|")
                .replace("%3B", ";")
                .replace("%7E", "~");
    }

    // =========================================================
    // BERSIHKAN RIWAYAT LEBIH DARI 1 TAHUN
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

        for (String nota : semua) {

            try {

                String[] bagian =
                        nota.split("\\|", 7);

                if (bagian.length < 7) {
                    continue;
                }

                long waktu =
                        Long.parseLong(
                                bagian[0]
                        );

                // Simpan jika belum 1 tahun
                if (sekarang - waktu <= SATU_TAHUN) {

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
    // TAMPILKAN RIWAYAT
    // =========================================================

    private void tampilkanRiwayat() {

        // Bersihkan terlebih dahulu
        bersihkanRiwayatLama();

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

            new AlertDialog.Builder(this)
                    .setTitle(
                            "📋 Riwayat Nota"
                    )
                    .setMessage(
                            "Belum ada nota tersimpan."
                    )
                    .setPositiveButton(
                            "OK",
                            null
                    )
                    .show();

            return;
        }

        String[] semua =
                data.split("\n");

        StringBuilder tampilan =
                new StringBuilder();

        for (int i = semua.length - 1;
             i >= 0;
             i--) {

            try {

                String[] bagian =
                        semua[i]
                                .split("\\|", 7);

                if (bagian.length < 7) {
                    continue;
                }

                String nama =
                        decode(bagian[1]);

                String wa =
                        decode(bagian[2]);

                String tanggal =
                        decode(bagian[3]);

                String motor =
                        decode(bagian[4]);

                long dp =
                        Long.parseLong(
                                bagian[5]
                        );

                long total = 0;

                String[] items =
                        bagian[6]
                                .split(";");

                for (String item : items) {

                    String[] isi =
                            item.split("~");

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
                }

                long sisa =
                        total - dp;

                if (sisa < 0) {
                    sisa = 0;
                }

                tampilan.append("👤 ")
                        .append(nama)
                        .append("\n");

                tampilan.append("📅 ")
                        .append(tanggal)
                        .append("\n");

                tampilan.append("📱 ")
                        .append(wa)
                        .append("\n");

                if (!motor.isEmpty()) {

                    tampilan.append("🏍️ ")
                            .append(motor)
                            .append("\n");
                }

                tampilan.append("💰 TOTAL : ")
                        .append(
                                formatRupiah(total)
                        )
                        .append("\n");

                tampilan.append("💵 DP : ")
                        .append(
                                formatRupiah(dp)
                        )
                        .append("\n");

                tampilan.append("💳 SISA : ")
                        .append(
                                formatRupiah(sisa)
                        )
                        .append("\n");

                if (sisa == 0) {

                    tampilan.append(
                            "✅ STATUS : LUNAS\n"
                    );

                } else {

                    tampilan.append(
                            "⚠️ STATUS : BELUM LUNAS\n"
                    );
                }

                tampilan.append(
                        "--------------------\n"
                );

            } catch (Exception ignored) {
            }
        }

        ScrollView scroll =
                new ScrollView(this);

        TextView teks =
                new TextView(this);

        teks.setText(
                tampilan.toString()
        );

        teks.setTextSize(16);

        teks.setPadding(
                25,
                20,
                25,
                20
        );

        scroll.addView(teks);

        new AlertDialog.Builder(this)
                .setTitle(
                        "📋 RIWAYAT NOTA"
                )
                .setView(scroll)
                .setPositiveButton(
                        "Tutup",
                        null
                )
                .show();
    }

    // =========================================================
    // BUAT TEKS NOTA UNTUK CETAK
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

        nota.append("================================\n");
        nota.append("          RR MOTOR\n");
        nota.append("       NOTA SERVIS\n");
        nota.append("================================\n\n");

        nota.append("Nama   : ")
                .append(nama)
                .append("\n");

        nota.append("WA     : ")
                .append(wa)
                .append("\n");

        nota.append("Tanggal: ")
                .append(tanggal)
                .append("\n");

        if (!motor.isEmpty()) {

            nota.append("Motor  : ")
                    .append(motor)
                    .append("\n");
        }

        nota.append("\n");
        nota.append("--------------------------------\n");
        nota.append("BARANG / JASA\n");
        nota.append("--------------------------------\n");

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            String barang =
                    namaBarang.get(i)
                            .getText()
                            .toString()
                            .trim();

            long jumlah =
                    angka(jumlahBarang.get(i));

            long harga =
                    angka(hargaBarang.get(i));

            if (!barang.isEmpty()) {

                nota.append(
                        barang
                ).append("\n");

                nota.append("  ")
                        .append(jumlah)
                        .append(" x ")
                        .append(
                                formatRupiah(harga)
                        )
                        .append("\n");

                nota.append("  SUBTOTAL: ")
                        .append(
                                formatRupiah(
                                        jumlah * harga
                                )
                        )
                        .append("\n\n");
            }
        }

        nota.append("--------------------------------\n");

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

        if (sisa == 0) {

            nota.append(
                    "STATUS: LUNAS\n"
            );

        } else {

            nota.append(
                    "STATUS: BELUM LUNAS\n"
            );
        }

        nota.append("\n");
        nota.append("--------------------------------\n");
        nota.append("Terima kasih.\n");
        nota.append("RR MOTOR\n");
        nota.append("================================\n");

        return nota.toString();
    }

    // =========================================================
    // CETAK NOTA
    // =========================================================

    private void cetakNota() {

        String nama =
                namaInput.getText()
                        .toString()
                        .trim();

        if (nama.isEmpty()) {

            namaInput.setError(
                    "Nama pelanggan wajib diisi"
            );

            namaInput.requestFocus();

            return;
        }

        String wa =
                waInput.getText()
                        .toString()
                        .trim();

        if (wa.isEmpty()) {

            waInput.setError(
                    "Nomor WhatsApp wajib diisi"
            );

            waInput.requestFocus();

            return;
        }

        String tanggal =
                tanggalInput.getText()
                        .toString()
                        .trim();

        if (tanggal.isEmpty()) {

            Toast.makeText(
                    this,
                    "Tanggal nota wajib diisi",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Pastikan total sudah dihitung
        hitungTotal();

        String isiNota =
                buatTeksNota();

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
                    "Gagal membuka menu cetak: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // WHATSAPP
    // =========================================================

    private void kirimWhatsApp() {

        String nama =
                namaInput.getText()
                        .toString()
                        .trim();

        String wa =
                waInput.getText()
                        .toString()
                        .trim();

        if (nama.isEmpty() ||
                wa.isEmpty()) {

            Toast.makeText(
                    this,
                    "Nama dan nomor WhatsApp wajib diisi",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

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
                    angka(jumlahBarang.get(i));

            long harga =
                    angka(hargaBarang.get(i));

            if (!barang.isEmpty()) {

                pesan.append("🔧 ")
                        .append(barang)
                        .append("\n");

                pesan.append("   ")
                        .append(jumlah)
                        .append(" x ")
                        .append(
                                formatRupiah(harga)
                        )
                        .append("\n");

                pesan.append("   = ")
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

        if (sisa == 0) {

            pesan.append(
                    "✅ *STATUS: LUNAS*\n\n"
            );

        } else {

            pesan.append(
                    "⚠️ *STATUS: BELUM LUNAS*\n\n"
            );
        }

        pesan.append(
                "Terima kasih sudah mempercayakan "
                        + "kendaraan Anda kepada "
                        + "*RR MOTOR*. 🙏"
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

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "WhatsApp tidak dapat dibuka",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
