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

    /*
     * Mencegah tombol cetak ditekan berkali-kali
     * ketika proses Bluetooth masih berjalan.
     */
    private volatile boolean sedangMencetak = false;

    private EditText namaInput;
    private EditText waInput;
    private EditText tanggalInput;
    private EditText motorInput;
    private EditText dpInput;

    private LinearLayout itemContainer;
    private ScrollView scrollView;

    private TextView totalText;
    private TextView sisaText;
    private TextView statusText;

    private final ArrayList<EditText> namaBarang = new ArrayList<>();
    private final ArrayList<EditText> jumlahBarang = new ArrayList<>();
    private final ArrayList<EditText> hargaBarang = new ArrayList<>();

    private static final String PREF_NAME = "RR_MOTOR_NOTA";
    private static final String KEY_HISTORY = "HISTORY";

    private static final long SATU_TAHUN =
            365L * 24L * 60L * 60L * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Supaya layar menyesuaikan saat keyboard muncul
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        bersihkanRiwayatLama();

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout utama = new LinearLayout(this);
        utama.setOrientation(LinearLayout.VERTICAL);
        utama.setPadding(25, 20, 25, 50);

        scrollView.addView(utama);

        // =========================
        // JUDUL
        // =========================

        TextView judul = new TextView(this);
        judul.setText("🏍️ RR MOTOR NOTA");
        judul.setTextSize(26);
        judul.setTypeface(null, Typeface.BOLD);
        judul.setGravity(Gravity.CENTER);
        judul.setPadding(0, 10, 0, 20);

        utama.addView(judul);

        // =========================
        // DATA PELANGGAN
        // =========================

        namaInput = buatInput("Nama pelanggan *");
        namaInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        utama.addView(namaInput);

        waInput = buatInput("Nomor WhatsApp *");
        waInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );
        utama.addView(waInput);

        tanggalInput = buatInput("Tanggal nota *");
        tanggalInput.setFocusable(false);
        tanggalInput.setClickable(true);
        tanggalInput.setOnClickListener(v -> pilihTanggal());
        utama.addView(tanggalInput);

        motorInput = buatInput("Jenis motor (opsional)");
        motorInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        utama.addView(motorInput);

        dpInput = buatInput("DP / Uang muka (opsional)");
        dpInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );
        utama.addView(dpInput);

        // =========================
        // DAFTAR BARANG
        // =========================

        TextView judulItem = new TextView(this);
        judulItem.setText("🧾 DAFTAR BARANG / JASA");
        judulItem.setTextSize(20);
        judulItem.setTypeface(null, Typeface.BOLD);
        judulItem.setPadding(0, 25, 0, 10);
        utama.addView(judulItem);

        itemContainer = new LinearLayout(this);
        itemContainer.setOrientation(
                LinearLayout.VERTICAL
        );
        utama.addView(itemContainer);

        Button tambahItem = new Button(this);
        tambahItem.setText(
                "＋ TAMBAH BARANG / JASA"
        );
        tambahItem.setTextSize(16);
        tambahItem.setOnClickListener(
                v -> tambahBarisItem()
        );
        utama.addView(tambahItem);

        // =========================
        // TOTAL
        // =========================

        totalText = new TextView(this);
        totalText.setTextSize(20);
        totalText.setTypeface(null, Typeface.BOLD);
        totalText.setPadding(0, 20, 0, 5);
        utama.addView(totalText);

        sisaText = new TextView(this);
        sisaText.setTextSize(20);
        sisaText.setTypeface(null, Typeface.BOLD);
        utama.addView(sisaText);

        statusText = new TextView(this);
        statusText.setTextSize(18);
        statusText.setPadding(0, 5, 0, 15);
        utama.addView(statusText);

        // =========================
        // SIMPAN
        // =========================

        Button simpan = new Button(this);
        simpan.setText("💾 SIMPAN NOTA");
        simpan.setTextSize(16);
        simpan.setOnClickListener(
                v -> simpanNota()
        );
        utama.addView(simpan);

        // =========================
        // CETAK BLUETOOTH
        // =========================

        Button cetakBluetooth = new Button(this);
        cetakBluetooth.setText(
                "🔵🖨️ CETAK BLUETOOTH"
        );
        cetakBluetooth.setTextSize(17);
        cetakBluetooth.setOnClickListener(
                v -> mulaiCetakBluetooth()
        );
        utama.addView(cetakBluetooth);

        // =========================
        // CETAK ANDROID / PDF
        // =========================

        Button cetak = new Button(this);
        cetak.setText(
                "🖨️ CETAK NOTA / PDF"
        );
        cetak.setTextSize(16);
        cetak.setOnClickListener(
                v -> cetakNota()
        );
        utama.addView(cetak);

        // =========================
        // RIWAYAT
        // =========================

        Button riwayat = new Button(this);
        riwayat.setText(
                "📋 RIWAYAT NOTA"
        );
        riwayat.setTextSize(16);
        riwayat.setOnClickListener(
                v -> tampilkanRiwayat()
        );
        utama.addView(riwayat);

        // =========================
        // WHATSAPP
        // =========================

        Button whatsapp = new Button(this);
        whatsapp.setText(
                "💬 KIRIM VIA WHATSAPP"
        );
        whatsapp.setTextSize(16);
        whatsapp.setOnClickListener(
                v -> kirimWhatsApp()
        );
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

        pasangListenerHitung(dpInput);

        hitungTotal();
    }

    // =========================================================
    // INPUT
    // =========================================================

    private EditText buatInput(String hint) {

        EditText input = new EditText(this);

        input.setHint(hint);
        input.setTextSize(17);
        input.setSingleLine(true);
        input.setFocusable(true);
        input.setFocusableInTouchMode(true);
        input.setClickable(true);
        input.setLongClickable(true);
        input.setPadding(20, 12, 20, 12);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 6, 0, 6);

        input.setLayoutParams(params);

        return input;
    }

    // =========================================================
    // LISTENER HITUNG
    // =========================================================

    private void pasangListenerHitung(EditText input) {

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
                        (view, year, month, day) -> {

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

        baris.setPadding(0, 8, 0, 8);

        EditText nama =
                buatInput(
                        "Nama barang / jasa"
                );

        nama.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        EditText jumlah =
                buatInput("Jumlah");

        jumlah.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        EditText harga =
                buatInput("Harga satuan");

        harga.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        Button hapus =
                new Button(this);

        hapus.setText(
                "🗑️ HAPUS ITEM"
        );

        baris.addView(nama);
        baris.addView(jumlah);
        baris.addView(harga);
        baris.addView(hapus);

        itemContainer.addView(baris);

        namaBarang.add(nama);
        jumlahBarang.add(jumlah);
        hargaBarang.add(harga);

        pasangListenerHitung(jumlah);
        pasangListenerHitung(harga);

        hapus.setOnClickListener(v -> {

            int posisi =
                    namaBarang.indexOf(nama);

            if (namaBarang.size() > 1) {

                if (posisi >= 0) {

                    namaBarang.remove(posisi);
                    jumlahBarang.remove(posisi);
                    hargaBarang.remove(posisi);

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
        });

        // Setelah menambah item,
        // fokus ke nama barang
        nama.requestFocus();
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

            total += jumlah * harga;
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
    // RUPIAH
    // =========================================================

    private String formatRupiah(long angka) {

        NumberFormat format =
                NumberFormat.getNumberInstance(
                        new Locale("id", "ID")
                );

        return "Rp " +
                format.format(angka);
    }

    // =========================================================
    // SIMPAN
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

        if (nama.isEmpty()) {

            namaInput.setError(
                    "Nama wajib diisi"
            );

            namaInput.requestFocus();
            tampilkanKeyboard(
                    namaInput
            );

            return;
        }

        if (wa.isEmpty()) {

            waInput.setError(
                    "Nomor WhatsApp wajib diisi"
            );

            waInput.requestFocus();
            tampilkanKeyboard(
                    waInput
            );

            return;
        }

        if (tanggal.isEmpty()) {

            Toast.makeText(
                    this,
                    "Tanggal wajib diisi",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

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
    // BERSIHKAN RIWAYAT 1 TAHUN
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
    // RIWAYAT
    // =========================================================

    private void tampilkanRiwayat() {

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
                            "📋 RIWAYAT NOTA"
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

                tampilan.append(
                        "👤 "
                )
                .append(nama)
                .append("\n");

                tampilan.append(
                        "📅 "
                )
                .append(tanggal)
                .append("\n");

                tampilan.append(
                        "📱 "
                )
                .append(wa)
                .append("\n");

                if (!motor.isEmpty()) {

                    tampilan.append(
                            "🏍️ "
                    )
                    .append(motor)
                    .append("\n");
                }

                tampilan.append(
                        "💰 TOTAL : "
                )
                .append(
                        formatRupiah(total)
                )
                .append("\n");

                tampilan.append(
                        "💵 DP : "
                )
                .append(
                        formatRupiah(dp)
                )
                .append("\n");

                tampilan.append(
                        "💳 SISA : "
                )
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
    // TEKS NOTA
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
                        formatRupiah(harga)
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
                    "Gagal membuka menu cetak: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // VALIDASI NOTA
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
                        formatRupiah(harga)
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

    // =========================================================
    // BLUETOOTH
    // =========================================================

    private void mulaiCetakBluetooth() {

        if (!validasiNota()) {
            return;
        }

        /*
         * Android 12 (API 31) ke atas:
         * izin perangkat Bluetooth harus diberikan.
         */
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
                                BluetoothAdapter
                                        .ACTION_REQUEST_ENABLE
                        );

                startActivity(intent);

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
                                "Belum ada perangkat Bluetooth "
                                        + "yang dipasangkan.\n\n"
                                        + "Pasangkan printer terlebih dahulu "
                                        + "melalui Pengaturan Bluetooth HP."
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

                daftar.add(device);

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
                                        daftar.get(which);

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
    // CETAK KE PRINTER BLUETOOTH
    // =========================================================

    private void cetakKePrinter(
            BluetoothDevice device) {

        /*
         * Jangan izinkan dua proses cetak Bluetooth
         * berjalan bersamaan.
         */
        if (sedangMencetak) {

            Toast.makeText(
                    MainActivity.this,
                    "Sedang mencetak, tunggu sebentar...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (device == null) {

            Toast.makeText(
                    MainActivity.this,
                    "Printer tidak ditemukan",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        sedangMencetak = true;

        /*
         * Buat teks nota sebelum masuk background thread.
         */
        final String isiNota =
                buatTeksNota();

        Toast.makeText(
                MainActivity.this,
                "🔵 Menghubungkan ke printer...",
                Toast.LENGTH_SHORT
        ).show();

        new Thread(() -> {

            BluetoothPrinter printer = null;

            try {

                // =================================================
                // BUAT OBJECT PRINTER
                // =================================================

                printer =
                        new BluetoothPrinter(
                                device
                        );

                // =================================================
                // CONNECT
                // =================================================

                printer.connect();

                // =================================================
                // PRINT
                // =================================================

                printer.print(
                        isiNota
                );

                // =================================================
                // BERHASIL
                // =================================================

                runOnUiThread(() -> {

                    Toast.makeText(
                            MainActivity.this,
                            "✅ Nota berhasil dicetak",
                            Toast.LENGTH_LONG
                    ).show();

                });

            } catch (Exception e) {

                String pesanError =
                        e.getMessage();

                if (pesanError == null ||
                        pesanError.trim().isEmpty()) {

                    pesanError =
                            "Koneksi printer gagal";
                }

                final String pesan =
                        pesanError;

                runOnUiThread(() -> {

                    Toast.makeText(
                            MainActivity.this,
                            "❌ Gagal mencetak:\n"
                                    + pesan,
                            Toast.LENGTH_LONG
                    ).show();

                });

            } finally {

                /*
                 * PENTING:
                 * Printer selalu diputus dan socket
                 * selalu dibersihkan, baik berhasil
                 * maupun gagal.
                 */
                if (printer != null) {

                    try {
                        printer.disconnect();
                    } catch (Exception ignored) {
                    }
                }

                sedangMencetak = false;
            }

        }).start();
    }

    // =========================================================
    // HASIL IZIN BLUETOOTH
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

                /*
                 * Setelah izin diberikan,
                 * ulangi proses cetak Bluetooth.
                 */
                mulaiCetakBluetooth();

            } else {

                Toast.makeText(
                        this,
                        "Izin Bluetooth diperlukan "
                                + "untuk mencetak nota",
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

        input.postDelayed(() -> {

            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                            getSystemService(
                                    Context.INPUT_METHOD_SERVICE
                            );

            if (imm != null) {

                imm.showSoftInput(
                        input,
                        android.view.inputmethod
                                .InputMethodManager
                                .SHOW_IMPLICIT
                );
            }

            scrollView.postDelayed(() -> {

                scrollView.smoothScrollTo(
                        0,
                        input.getBottom()
                );

            }, 200);

        }, 150);
    }
}
