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
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;

public class MainActivity extends Activity {

    private static final int REQUEST_BLUETOOTH = 1001;
    private static final int REQUEST_PILIH_KONTAK = 1002;

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

    private long editingTimestamp = 0;

    private String statusBayar =
            "BELUM LUNAS";

    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private FirebaseUser userAktif;

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActionBar() != null) {
            getActionBar().hide();
        }

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        firebaseAuth =
                FirebaseAuth.getInstance();

        firestore =
                FirebaseFirestore.getInstance();

        userAktif =
                firebaseAuth.getCurrentUser();

        if (userAktif == null) {

            tampilkanLogin();

        } else {

            tampilkanMenuUtama();
        }
    }

    // =========================================================
    // LOGIN FIREBASE
    // =========================================================

    private void tampilkanLogin() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                30,
                20,
                30,
                20
        );

        TextView judul =
                new TextView(this);

        judul.setText(
                "🏍️ RR MOTOR"
        );

        judul.setTextSize(24);

        judul.setTypeface(
                null,
                Typeface.BOLD
        );

        judul.setGravity(
                Gravity.CENTER
        );

        judul.setPadding(
                0,
                0,
                0,
                20
        );

        layout.addView(judul);

        TextView keterangan =
                new TextView(this);

        keterangan.setText(
                "LOGIN AKUN RR MOTOR"
        );

        keterangan.setTextSize(17);

        keterangan.setTypeface(
                null,
                Typeface.BOLD
        );

        keterangan.setGravity(
                Gravity.CENTER
        );

        keterangan.setPadding(
                0,
                0,
                0,
                15
        );

        layout.addView(keterangan);

        EditText email =
                new EditText(this);

        email.setHint(
                "Email"
        );

        email.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        email.setSingleLine(true);

        layout.addView(email);

        EditText password =
                new EditText(this);

        password.setHint(
                "Password"
        );

        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        password.setSingleLine(true);

        layout.addView(password);

        Button login =
                new Button(this);

        login.setText(
                "🔐 LOGIN"
        );

        login.setTextSize(14);

        layout.addView(login);

        TextView info =
                new TextView(this);

        info.setText(
                "\nGunakan email dan password akun RR MOTOR."
        );

        info.setTextSize(13);

        info.setGravity(
                Gravity.CENTER
        );

        layout.addView(info);

        setContentView(layout);

        login.setOnClickListener(
                v -> {

                    String emailText =
                            email.getText()
                                    .toString()
                                    .trim();

                    String passwordText =
                            password.getText()
                                    .toString();

                    if (emailText.isEmpty()) {

                        email.setError(
                                "Email wajib diisi"
                        );

                        email.requestFocus();

                        return;
                    }

                    if (passwordText.isEmpty()) {

                        password.setError(
                                "Password wajib diisi"
                        );

                        password.requestFocus();

                        return;
                    }

                    login.setEnabled(false);

                    login.setText(
                            "⏳ LOGIN..."
                    );

                    firebaseAuth
                            .signInWithEmailAndPassword(
                                    emailText,
                                    passwordText
                            )
                            .addOnCompleteListener(
                                    task -> {

                                        if (task.isSuccessful()) {

                                            userAktif =
                                                    firebaseAuth
                                                            .getCurrentUser();

                                            Toast.makeText(
                                                    this,
                                                    "Login berhasil ✅",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            tampilkanMenuUtama();

                                        } else {

                                            login.setEnabled(
                                                    true
                                            );

                                            login.setText(
                                                    "🔐 LOGIN"
                                            );

                                            String pesan =
                                                    task.getException() != null
                                                            ? task.getException()
                                                                    .getMessage()
                                                            : "Login gagal";

                                            Toast.makeText(
                                                    this,
                                                    "Login gagal: " +
                                                            pesan,
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                            );
                }
        );
    }

    // =========================================================
    // MENU UTAMA
    // =========================================================

    private void tampilkanMenuUtama() {

        if (firebaseAuth.getCurrentUser() == null) {

            tampilkanLogin();

            return;
        }

        userAktif =
                firebaseAuth.getCurrentUser();

        editingTimestamp = 0;

        scrollView = new ScrollView(this);

        scrollView.setFillViewport(true);

        scrollView.setFocusable(true);
        scrollView.setFocusableInTouchMode(true);

        scrollView.setClipToPadding(false);

        LinearLayout utama =
                new LinearLayout(this);

        utama.setOrientation(
                LinearLayout.VERTICAL
        );

        utama.setPadding(
                20,
                8,
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

        judul.setTextSize(17);

        judul.setTypeface(
                null,
                Typeface.BOLD
        );

        judul.setGravity(
                Gravity.CENTER
        );

        judul.setPadding(
                0,
                2,
                0,
                6
        );

        utama.addView(judul);

        // =====================================================
        // INFO AKUN
        // =====================================================

        TextView akunText =
                new TextView(this);

        akunText.setText(
                "☁️ Akun: " +
                        userAktif.getEmail()
        );

        akunText.setTextSize(12);

        akunText.setGravity(
                Gravity.CENTER
        );

        akunText.setPadding(
                0,
                0,
                0,
                5
        );

        utama.addView(akunText);

        // =====================================================
        // NAMA PELANGGAN
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

        // =====================================================
        // WHATSAPP
        // =====================================================

        LinearLayout barisWhatsApp =
                new LinearLayout(this);

        barisWhatsApp.setOrientation(
                LinearLayout.HORIZONTAL
        );

        barisWhatsApp.setGravity(
                Gravity.CENTER_VERTICAL
        );

        waInput =
                buatInput(
                        "Nomor WhatsApp *"
                );

        waInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        LinearLayout.LayoutParams waParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        waParams.setMargins(
                0,
                3,
                5,
                3
        );

        waInput.setLayoutParams(
                waParams
        );

        barisWhatsApp.addView(
                waInput
        );

        Button ambilKontak =
                new Button(this);

        ambilKontak.setText(
                "📱 KONTAK"
        );

        ambilKontak.setTextSize(11);

        ambilKontak.setAllCaps(false);

        barisWhatsApp.addView(
                ambilKontak
        );

        ambilKontak.setOnClickListener(
                v -> pilihKontak()
        );

        utama.addView(
                barisWhatsApp
        );

        // =====================================================
        // TANGGAL
        // =====================================================

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

        // =====================================================
        // MOTOR
        // =====================================================

        motorInput =
                buatInput(
                        "Jenis motor (opsional)"
                );

        motorInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );

        utama.addView(motorInput);

        // =====================================================
        // DP
        // =====================================================

        dpInput =
                buatInput(
                        "DP / Uang muka (opsional)"
                );

        dpInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        utama.addView(dpInput);

        // =====================================================
        // STATUS
        // =====================================================

        TextView statusLabel =
                new TextView(this);

        statusLabel.setText(
                "Status pembayaran"
        );

        statusLabel.setTextSize(15);

        statusLabel.setTypeface(
                null,
                Typeface.BOLD
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
        // ITEM
        // =====================================================

        TextView judulItem =
                new TextView(this);

        judulItem.setText(
                "🧾 DAFTAR BARANG / JASA"
        );

        judulItem.setTextSize(17);

        judulItem.setTypeface(
                null,
                Typeface.BOLD
        );

        judulItem.setPadding(
                0,
                10,
                0,
                5
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

        tambahItem.setTextSize(13);

        tambahItem.setOnClickListener(
                v -> tambahBarisItem()
        );

        utama.addView(tambahItem);

        // =====================================================
        // TOTAL
        // =====================================================

        totalText =
                new TextView(this);

        totalText.setTextSize(17);

        totalText.setTypeface(
                null,
                Typeface.BOLD
        );

        utama.addView(totalText);

        sisaText =
                new TextView(this);

        sisaText.setTextSize(17);

        sisaText.setTypeface(
                null,
                Typeface.BOLD
        );

        utama.addView(sisaText);

        statusText =
                new TextView(this);

        statusText.setTextSize(15);

        statusText.setTypeface(
                null,
                Typeface.BOLD
        );

        utama.addView(statusText);

        // =====================================================
        // SIMPAN
        // =====================================================

        Button simpan =
                new Button(this);

        simpan.setText(
                "💾 SIMPAN NOTA KE CLOUD"
        );

        simpan.setTextSize(13);

        simpan.setOnClickListener(
                v -> simpanNota()
        );

        utama.addView(simpan);

        // =====================================================
        // BLUETOOTH
        // =====================================================

        Button cetakBluetooth =
                new Button(this);

        cetakBluetooth.setText(
                "🔵🖨️ CETAK BLUETOOTH"
        );

        cetakBluetooth.setTextSize(13);

        cetakBluetooth.setOnClickListener(
                v -> mulaiCetakBluetooth()
        );

        utama.addView(cetakBluetooth);

        // =====================================================
        // PDF
        // =====================================================

        Button cetak =
                new Button(this);

        cetak.setText(
                "🖨️ CETAK NOTA / PDF"
        );

        cetak.setTextSize(13);

        cetak.setOnClickListener(
                v -> cetakNota()
        );

        utama.addView(cetak);

        // =====================================================
        // RIWAYAT
        // =====================================================

        Button riwayat =
                new Button(this);

        riwayat.setText(
                "📋 RIWAYAT NOTA CLOUD"
        );

        riwayat.setTextSize(13);

        riwayat.setOnClickListener(
                v -> tampilkanRiwayat()
        );

        utama.addView(riwayat);

        // =====================================================
        // WHATSAPP
        // =====================================================

        Button whatsapp =
                new Button(this);

        whatsapp.setText(
                "💬 KIRIM VIA WHATSAPP"
        );

        whatsapp.setTextSize(13);

        whatsapp.setOnClickListener(
                v -> kirimWhatsApp()
        );

        utama.addView(whatsapp);

        // =====================================================
        // LOGOUT
        // =====================================================

        Button logout =
                new Button(this);

        logout.setText(
                "🚪 KELUAR AKUN"
        );

        logout.setTextSize(12);

        logout.setOnClickListener(
                v -> {

                    new AlertDialog.Builder(this)
                            .setTitle(
                                    "Keluar akun?"
                            )
                            .setMessage(
                                    "Anda akan keluar dari akun RR MOTOR."
                            )
                            .setNegativeButton(
                                    "BATAL",
                                    null
                            )
                            .setPositiveButton(
                                    "KELUAR",
                                    (dialog, which) -> {

                                        firebaseAuth.signOut();

                                        userAktif =
                                                null;

                                        tampilkanLogin();
                                    }
                            )
                            .show();
                }
        );

        utama.addView(logout);

        // =====================================================
        // PASANG LAYAR
        // =====================================================

        setContentView(scrollView);

        tanggalInput.setText(
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                ).format(
                        new Date()
                )
        );

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
        input.setTextSize(15);
        input.setSingleLine(true);

        input.setFocusable(true);
        input.setFocusableInTouchMode(true);
        input.setClickable(true);

        input.setPadding(
                12,
                6,
                12,
                6
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                2,
                0,
                2
        );

        input.setLayoutParams(params);

        input.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {

                        v.postDelayed(
                                () -> pastikanInputTerlihat(v),
                                200
                        );
                    }
                }
        );

        input.setOnClickListener(
                v -> {

                    v.postDelayed(
                            () -> pastikanInputTerlihat(v),
                            150
                    );
                }
        );

        return input;
    }

    // =========================================================
    // SCROLL INPUT
    // =========================================================

    private void pastikanInputTerlihat(
            View input) {

        if (scrollView == null ||
                input == null) {
            return;
        }

        try {

            android.graphics.Rect rect =
                    new android.graphics.Rect();

            input.getDrawingRect(rect);

            scrollView.offsetDescendantRectToMyCoords(
                    input,
                    rect
            );

            int tinggiLayar =
                    scrollView.getHeight();

            if (rect.bottom >
                    tinggiLayar - 30) {

                scrollView.smoothScrollBy(
                        0,
                        rect.bottom -
                                tinggiLayar +
                                70
                );

            } else if (rect.top < 20) {

                scrollView.smoothScrollBy(
                        0,
                        rect.top - 50
                );
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // KONTAK
    // =========================================================

    private void pilihKontak() {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_PICK,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    );

            startActivityForResult(
                    intent,
                    REQUEST_PILIH_KONTAK
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Kontak HP tidak dapat dibuka",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode ==
                REQUEST_PILIH_KONTAK &&
                resultCode ==
                        RESULT_OK &&
                data != null &&
                data.getData() != null) {

            Cursor cursor = null;

            try {

                cursor =
                        getContentResolver()
                                .query(
                                        data.getData(),
                                        new String[]{
                                                ContactsContract.CommonDataKinds.Phone.NUMBER
                                        },
                                        null,
                                        null,
                                        null
                                );

                if (cursor != null &&
                        cursor.moveToFirst()) {

                    int index =
                            cursor.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER
                            );

                    if (index >= 0) {

                        String nomor =
                                cursor.getString(index);

                        if (nomor != null) {

                            waInput.setText(
                                    nomor.trim()
                            );

                            waInput.setSelection(
                                    waInput.length()
                            );
                        }
                    }
                }

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Gagal mengambil nomor kontak",
                        Toast.LENGTH_LONG
                ).show();

            } finally {

                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    // =========================================================
    // HITUNG
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

                            tanggalInput.setText(
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d/%02d/%04d",
                                            day,
                                            month + 1,
                                            year
                                    )
                            );
                        },
                        kalender.get(Calendar.YEAR),
                        kalender.get(Calendar.MONTH),
                        kalender.get(Calendar.DAY_OF_MONTH)
                );

        dialog.show();
    }

    // =========================================================
    // ITEM
    // =========================================================

    private void tambahBarisItem() {

        LinearLayout baris =
                new LinearLayout(this);

        baris.setOrientation(
                LinearLayout.VERTICAL
        );

        EditText nama =
                buatInput(
                        "Nama barang / jasa"
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

        hapus.setTextSize(12);

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

        hapus.setOnClickListener(
                v -> {

                    int posisi =
                            namaBarang.indexOf(nama);

                    if (namaBarang.size() > 1 &&
                            posisi >= 0) {

                        namaBarang.remove(posisi);
                        jumlahBarang.remove(posisi);
                        hargaBarang.remove(posisi);

                        itemContainer.removeView(baris);

                        hitungTotal();

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

    private long angka(
            EditText input) {

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

    private long hitungTotal() {

        long total = 0;

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            total +=
                    angka(jumlahBarang.get(i)) *
                            angka(hargaBarang.get(i));
        }

        long dp =
                angka(dpInput);

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

    private void perbaruiTampilanStatus() {

        if (statusText == null) {
            return;
        }

        if ("LUNAS".equals(statusBayar)) {

            statusText.setText(
                    "STATUS : LUNAS ✅"
            );

        } else {

            statusText.setText(
                    "STATUS : BELUM LUNAS ⚠️"
            );
        }
    }

    private String formatRupiah(
            long angka) {

        NumberFormat format =
                NumberFormat.getNumberInstance(
                        new Locale("id", "ID")
                );

        return "Rp " +
                format.format(angka);
    }

    // =========================================================
    // SIMPAN KE FIRESTORE
    // =========================================================

    private void simpanNota() {

        if (!validasiNota()) {
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Silakan login terlebih dahulu",
                    Toast.LENGTH_LONG
            ).show();

            tampilkanLogin();

            return;
        }

        userAktif =
                firebaseAuth.getCurrentUser();

        long total =
                hitungTotal();

        long dp =
                angka(dpInput);

        long sisa =
                total - dp;

        if (sisa < 0) {
            sisa = 0;
        }

        long timestamp =
                editingTimestamp != 0
                        ? editingTimestamp
                        : System.currentTimeMillis();

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "timestamp",
                timestamp
        );

        data.put(
                "nama",
                namaInput.getText()
                        .toString()
                        .trim()
        );

        data.put(
                "wa",
                waInput.getText()
                        .toString()
                        .trim()
        );

        data.put(
                "tanggal",
                tanggalInput.getText()
                        .toString()
                        .trim()
        );

        data.put(
                "motor",
                motorInput.getText()
                        .toString()
                        .trim()
        );

        data.put(
                "dp",
                dp
        );

        data.put(
                "total",
                total
        );

        data.put(
                "sisa",
                sisa
        );

        data.put(
                "status",
                statusBayar
        );

        data.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        ArrayList<Map<String, Object>> items =
                new ArrayList<>();

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            String namaBarangItem =
                    namaBarang.get(i)
                            .getText()
                            .toString()
                            .trim();

            if (namaBarangItem.isEmpty()) {
                continue;
            }

            Map<String, Object> item =
                    new HashMap<>();

            item.put(
                    "nama",
                    namaBarangItem
            );

            item.put(
                    "jumlah",
                    angka(jumlahBarang.get(i))
            );

            item.put(
                    "harga",
                    angka(hargaBarang.get(i))
            );

            items.add(item);
        }

        data.put(
                "items",
                items
        );

        String uid =
                userAktif.getUid();

        firestore.collection(
                        "users"
                )
                .document(uid)
                .collection("notas")
                .document(
                        String.valueOf(timestamp)
                )
                .set(data)
                .addOnSuccessListener(
                        unused -> {

                            editingTimestamp = 0;

                            Toast.makeText(
                                    this,
                                    "Nota tersimpan ke Cloud ☁️✅",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    this,
                                    "Gagal menyimpan ke Firebase: " +
                                            e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    // =========================================================
    // RIWAYAT CLOUD
    // =========================================================

    private void tampilkanRiwayat() {

        if (firebaseAuth.getCurrentUser() == null) {

            tampilkanLogin();

            return;
        }

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                15,
                5,
                15,
                5
        );

        EditText cariInput =
                new EditText(this);

        cariInput.setHint(
                "🔎 Cari nama / nomor WhatsApp"
        );

        cariInput.setSingleLine(true);

        layout.addView(cariInput);

        Button tombolCari =
                new Button(this);

        tombolCari.setText(
                "🔎 CARI RIWAYAT"
        );

        layout.addView(tombolCari);

        LinearLayout daftar =
                new LinearLayout(this);

        daftar.setOrientation(
                LinearLayout.VERTICAL
        );

        ScrollView scrollDaftar =
                new ScrollView(this);

        scrollDaftar.addView(daftar);

        layout.addView(
                scrollDaftar,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        Button kembali =
                new Button(this);

        kembali.setText(
                "🔙 KEMBALI"
        );

        layout.addView(kembali);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "📋 RIWAYAT NOTA CLOUD"
                        )
                        .setView(layout)
                        .create();

        Runnable muat =
                () -> {

                    daftar.removeAllViews();

                    long batas =
                            System.currentTimeMillis()
                                    - SATU_TAHUN;

                    String kata =
                            cariInput.getText()
                                    .toString()
                                    .trim()
                                    .toLowerCase(
                                            Locale.getDefault()
                                    );

                    firestore.collection("users")
                            .document(
                                    firebaseAuth
                                            .getCurrentUser()
                                            .getUid()
                            )
                            .collection("notas")
                            .whereGreaterThanOrEqualTo(
                                    "timestamp",
                                    batas
                            )
                            .orderBy(
                                    "timestamp",
                                    Query.Direction.DESCENDING
                            )
                            .get()
                            .addOnSuccessListener(
                                    snapshot -> {

                                        if (snapshot.isEmpty()) {

                                            TextView kosong =
                                                    new TextView(this);

                                            kosong.setText(
                                                    "Belum ada nota di Cloud."
                                            );

                                            kosong.setTextSize(15);

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

                                        for (DocumentSnapshot doc :
                                                snapshot.getDocuments()) {

                                            String nama =
                                                    ambilString(
                                                            doc,
                                                            "nama"
                                                    );

                                            String wa =
                                                    ambilString(
                                                            doc,
                                                            "wa"
                                                    );

                                            if (!kata.isEmpty()) {

                                                boolean cocok =
                                                        nama.toLowerCase(
                                                                Locale.getDefault()
                                                        ).contains(kata)
                                                                ||
                                                                wa.toLowerCase(
                                                                        Locale.getDefault()
                                                                ).contains(kata);

                                                if (!cocok) {
                                                    continue;
                                                }
                                            }

                                            buatKartuCloud(
                                                    doc,
                                                    daftar,
                                                    dialog
                                            );
                                        }

                                        if (daftar.getChildCount()
                                                == 0) {

                                            TextView kosong =
                                                    new TextView(this);

                                            kosong.setText(
                                                    "Riwayat tidak ditemukan."
                                            );

                                            kosong.setTextSize(15);

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
                                    }
                            )
                            .addOnFailureListener(
                                    e -> {

                                        TextView error =
                                                new TextView(this);

                                        error.setText(
                                                "Gagal mengambil riwayat:\n" +
                                                        e.getMessage()
                                        );

                                        error.setTextSize(14);

                                        error.setPadding(
                                                10,
                                                20,
                                                10,
                                                20
                                        );

                                        daftar.addView(
                                                error
                                        );
                                    }
                            );
                };

        tombolCari.setOnClickListener(
                v -> muat.run()
        );

        kembali.setOnClickListener(
                v -> dialog.dismiss()
        );

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

        muat.run();
    }

    // =========================================================
    // KARTU RIWAYAT CLOUD
    // =========================================================

    private void buatKartuCloud(
            DocumentSnapshot doc,
            LinearLayout daftar,
            AlertDialog dialog) {

        String nama =
                ambilString(doc, "nama");

        String wa =
                ambilString(doc, "wa");

        String tanggal =
                ambilString(doc, "tanggal");

        String motor =
                ambilString(doc, "motor");

        long total =
                ambilLong(doc, "total");

        long dp =
                ambilLong(doc, "dp");

        long sisa =
                ambilLong(doc, "sisa");

        String status =
                ambilString(doc, "status");

        long timestamp =
                ambilLong(doc, "timestamp");

        LinearLayout kartu =
                new LinearLayout(this);

        kartu.setOrientation(
                LinearLayout.VERTICAL
        );

        kartu.setPadding(
                10,
                8,
                10,
                8
        );

        TextView info =
                new TextView(this);

        String teks =
                "👤 " + nama +
                        "\n📱 " + wa +
                        "\n📅 " + tanggal;

        if (!motor.isEmpty()) {

            teks +=
                    "\n🏍️ " + motor;
        }

        teks +=
                "\n💰 TOTAL : " +
                        formatRupiah(total) +
                        "\n💵 DP : " +
                        formatRupiah(dp) +
                        "\n💳 SISA : " +
                        formatRupiah(sisa) +
                        "\n";

        teks +=
                "LUNAS".equals(status)
                        ? "✅ STATUS : LUNAS"
                        : "⚠️ STATUS : BELUM LUNAS";

        info.setText(teks);

        info.setTextSize(14);

        kartu.addView(info);

        Button edit =
                new Button(this);

        edit.setText(
                "✏️ EDIT NOTA"
        );

        edit.setTextSize(11);

        kartu.addView(edit);

        edit.setOnClickListener(
                v -> {

                    dialog.dismiss();

                    muatNotaCloud(
                            timestamp,
                            false
                    );
                }
        );

        Button print =
                new Button(this);

        print.setText(
                "🖨️ PRINT BLUETOOTH"
        );

        print.setTextSize(11);

        kartu.addView(print);

        print.setOnClickListener(
                v -> {

                    dialog.dismiss();

                    muatNotaCloud(
                            timestamp,
                            true
                    );
                }
        );

        Button waButton =
                new Button(this);

        waButton.setText(
                "💬 KIRIM WHATSAPP"
        );

        waButton.setTextSize(11);

        kartu.addView(waButton);

        waButton.setOnClickListener(
                v -> {

                    dialog.dismiss();

                    muatNotaCloud(
                            timestamp,
                            false
                    );

                    Toast.makeText(
                            this,
                            "Nota dimuat. Tekan KIRIM VIA WHATSAPP.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        Button hapus =
                new Button(this);

        hapus.setText(
                "🗑️ HAPUS RIWAYAT"
        );

        hapus.setTextSize(11);

        kartu.addView(hapus);

        hapus.setOnClickListener(
                v -> {

                    new AlertDialog.Builder(this)
                            .setTitle(
                                    "Hapus nota?"
                            )
                            .setMessage(
                                    "Nota " +
                                            nama +
                                            " akan dihapus dari Cloud."
                            )
                            .setNegativeButton(
                                    "BATAL",
                                    null
                            )
                            .setPositiveButton(
                                    "HAPUS",
                                    (d, which) -> {

                                        hapusNotaCloud(
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

        kartu.addView(garis);

        daftar.addView(kartu);
    }

    // =========================================================
    // AMBIL NOTA CLOUD
    // =========================================================

    private void muatNotaCloud(
            long timestamp,
            boolean langsungPrint) {

        firestore.collection("users")
                .document(
                        firebaseAuth
                                .getCurrentUser()
                                .getUid()
                )
                .collection("notas")
                .document(
                        String.valueOf(timestamp)
                )
                .get()
                .addOnSuccessListener(
                        doc -> {

                            if (!doc.exists()) {

                                Toast.makeText(
                                        this,
                                        "Nota tidak ditemukan.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            tampilkanMenuUtama();

                            if (muatDataCloud(doc)) {

                                editingTimestamp =
                                        timestamp;

                                if (langsungPrint) {

                                    mulaiCetakBluetooth();
                                }
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    this,
                                    "Gagal mengambil nota: " +
                                            e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    // =========================================================
    // MUAT DATA CLOUD
    // =========================================================

    private boolean muatDataCloud(
            DocumentSnapshot doc) {

        try {

            namaInput.setText(
                    ambilString(doc, "nama")
            );

            waInput.setText(
                    ambilString(doc, "wa")
            );

            tanggalInput.setText(
                    ambilString(doc, "tanggal")
            );

            motorInput.setText(
                    ambilString(doc, "motor")
            );

            dpInput.setText(
                    String.valueOf(
                            ambilLong(doc, "dp")
                    )
            );

            statusBayar =
                    ambilString(doc, "status");

            if (!"LUNAS".equals(statusBayar)) {

                statusBayar =
                        "BELUM LUNAS";
            }

            if (statusSpinner != null) {

                statusSpinner.setSelection(
                        "LUNAS".equals(statusBayar)
                                ? 1
                                : 0
                );
            }

            itemContainer.removeAllViews();

            namaBarang.clear();
            jumlahBarang.clear();
            hargaBarang.clear();

            List<Map<String, Object>> items =
                    (List<Map<String, Object>>)
                            doc.get("items");

            boolean adaItem = false;

            if (items != null) {

                for (Map<String, Object> item :
                        items) {

                    tambahBarisItem();

                    int posisi =
                            namaBarang.size() - 1;

                    namaBarang.get(posisi)
                            .setText(
                                    item.get("nama") != null
                                            ? String.valueOf(
                                                    item.get("nama")
                                            )
                                            : ""
                            );

                    jumlahBarang.get(posisi)
                            .setText(
                                    String.valueOf(
                                            angkaDariObject(
                                                    item.get("jumlah")
                                            )
                                    )
                            );

                    hargaBarang.get(posisi)
                            .setText(
                                    String.valueOf(
                                            angkaDariObject(
                                                    item.get("harga")
                                            )
                                    )
                            );

                    adaItem = true;
                }
            }

            if (!adaItem) {
                tambahBarisItem();
            }

            hitungTotal();

            return true;

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Gagal membaca nota: " +
                            e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }
    }

    // =========================================================
    // HAPUS CLOUD
    // =========================================================

    private void hapusNotaCloud(
            long timestamp) {

        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        firestore.collection("users")
                .document(
                        firebaseAuth
                                .getCurrentUser()
                                .getUid()
                )
                .collection("notas")
                .document(
                        String.valueOf(timestamp)
                )
                .delete()
                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    this,
                                    "Nota berhasil dihapus dari Cloud 🗑️",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    this,
                                    "Gagal menghapus: " +
                                            e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    // =========================================================
    // FIRESTORE HELPER
    // =========================================================

    private String ambilString(
            DocumentSnapshot doc,
            String field) {

        Object value =
                doc.get(field);

        return value == null
                ? ""
                : String.valueOf(value);
    }

    private long ambilLong(
            DocumentSnapshot doc,
            String field) {

        Object value =
                doc.get(field);

        return angkaDariObject(value);
    }

    private long angkaDariObject(
            Object value) {

        if (value instanceof Number) {

            return ((Number) value)
                    .longValue();
        }

        try {

            return Long.parseLong(
                    String.valueOf(value)
            );

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // TEKS NOTA
    // =========================================================

    private String buatTeksNota() {

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
        .append(
                namaInput.getText()
                        .toString()
        )
        .append("\n");

        nota.append(
                "WA     : "
        )
        .append(
                waInput.getText()
                        .toString()
        )
        .append("\n");

        nota.append(
                "Tanggal: "
        )
        .append(
                tanggalInput.getText()
                        .toString()
        )
        .append("\n");

        String motor =
                motorInput.getText()
                        .toString()
                        .trim();

        if (!motor.isEmpty()) {

            nota.append(
                    "Motor  : "
            )
            .append(motor)
            .append("\n");
        }

        nota.append(
                "\n--------------------------------\n"
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

            if (barang.isEmpty()) {
                continue;
            }

            long jumlah =
                    angka(jumlahBarang.get(i));

            long harga =
                    angka(hargaBarang.get(i));

            nota.append(barang)
                    .append("\n");

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

        nota.append(
                "STATUS: "
        )
        .append(
                "LUNAS".equals(statusBayar)
                        ? "LUNAS\n"
                        : "BELUM LUNAS\n"
        );

        nota.append(
                "\nTerima kasih.\n"
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
    // CETAK PDF
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
                return;
            }

            NotaPrintAdapter adapter =
                    new NotaPrintAdapter(
                            buatTeksNota()
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

        if (namaInput.getText()
                .toString()
                .trim()
                .isEmpty()) {

            namaInput.setError(
                    "Nama pelanggan wajib diisi"
            );

            namaInput.requestFocus();

            tampilkanKeyboard(namaInput);

            return false;
        }

        if (waInput.getText()
                .toString()
                .trim()
                .isEmpty()) {

            waInput.setError(
                    "Nomor WhatsApp wajib diisi"
            );

            waInput.requestFocus();

            tampilkanKeyboard(waInput);

            return false;
        }

        if (tanggalInput.getText()
                .toString()
                .trim()
                .isEmpty()) {

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

            if (barang.isEmpty()) {
                continue;
            }

            long jumlah =
                    angka(jumlahBarang.get(i));

            long harga =
                    angka(hargaBarang.get(i));

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

        pesan.append(
                "LUNAS".equals(statusBayar)
                        ? "✅ *STATUS: LUNAS*\n\n"
                        : "⚠️ *STATUS: BELUM LUNAS*\n\n"
        );

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
            return;
        }

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                    "https://wa.me/" +
                                            nomor +
                                            "?text=" +
                                            Uri.encode(
                                                    pesan.toString()
                                            )
                            )
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

        BluetoothManager manager =
                (BluetoothManager)
                        getSystemService(
                                Context.BLUETOOTH_SERVICE
                        );

        if (manager == null) {
            return;
        }

        BluetoothAdapter adapter =
                manager.getAdapter();

        if (adapter == null) {

            Toast.makeText(
                    this,
                    "Bluetooth tidak tersedia",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (!adapter.isEnabled()) {

            try {

                startActivity(
                        new Intent(
                                BluetoothAdapter.ACTION_REQUEST_ENABLE
                        )
                );

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Aktifkan Bluetooth HP",
                        Toast.LENGTH_LONG
                ).show();
            }

            return;
        }

        tampilkanPrinterBluetooth(adapter);
    }

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
                                "Belum ada printer Bluetooth yang dipasangkan."
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

                } catch (Exception e) {

                    namaPrinter =
                            "Printer Bluetooth";
                }

                if (namaPrinter == null ||
                        namaPrinter.trim().isEmpty()) {

                    namaPrinter =
                            "Printer Bluetooth";
                }

                nama.add(
                        namaPrinter +
                                "\n" +
                                device.getAddress()
                );
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            "🔵 PILIH PRINTER"
                    )
                    .setItems(
                            nama.toArray(
                                    new String[0]
                            ),
                            (dialog, which) ->
                                    cetakKePrinter(
                                            daftar.get(which)
                                    )
                    )
                    .setNegativeButton(
                            "BATAL",
                            null
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Izin Bluetooth belum tersedia",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void cetakKePrinter(
            BluetoothDevice device) {

        String isiNota =
                buatTeksNota();

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
                                () ->
                                        Toast.makeText(
                                                MainActivity.this,
                                                "Nota berhasil dicetak ✅",
                                                Toast.LENGTH_LONG
                                        ).show()
                        );

                    } catch (Exception e) {

                        runOnUiThread(
                                () ->
                                        Toast.makeText(
                                                MainActivity.this,
                                                "Gagal mencetak: " +
                                                        e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show()
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

            boolean ok = true;

            for (int result :
                    grantResults) {

                if (result !=
                        PackageManager.PERMISSION_GRANTED) {

                    ok = false;

                    break;
                }
            }

            if (ok) {

                mulaiCetakBluetooth();

            } else {

                Toast.makeText(
                        this,
                        "Izin Bluetooth diperlukan",
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

        if (input == null) {
            return;
        }

        input.requestFocus();

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

                    pastikanInputTerlihat(input);

                },
                150
        );
    }
}
