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
                        kalender.get(Calendar.MONTH
