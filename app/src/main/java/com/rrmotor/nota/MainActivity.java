package com.rrmotor.nota;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    private final ArrayList<EditText> namaBarang = new ArrayList<>();
    private final ArrayList<EditText> jumlahBarang = new ArrayList<>();
    private final ArrayList<EditText> hargaBarang = new ArrayList<>();

    private static final String PREF_NAME = "RR_MOTOR_NOTA";
    private static final String KEY_HISTORY = "HISTORY";
    private static final long SATU_TAHUN =
            365L * 24L * 60L * 60L * 1000L;

    private long editingTimestamp = 0;
    private String statusBayar = "BELUM LUNAS";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bersihkanRiwayatLama();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            tampilkanLogin();
        } else {
            tampilkanMenuUtama();
            migrasiRiwayatLokalKeCloud();
        }
    }

    // ============================================================
    // LOGIN FIREBASE
    // ============================================================

    private void tampilkanLogin() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 50, 40, 40);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);

        TextView judul = new TextView(this);
        judul.setText("🏍️ RR MOTOR");
        judul.setTextSize(30);
        judul.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        judul.setGravity(Gravity.CENTER);
        judul.setPadding(0, 20, 0, 10);

        root.addView(judul,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView subjudul = new TextView(this);
        subjudul.setText("NOTA ONLINE");
        subjudul.setTextSize(18);
        subjudul.setGravity(Gravity.CENTER);
        subjudul.setPadding(0, 0, 0, 35);

        root.addView(subjudul,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        EditText emailInput = buatInputLogin(
                "Email",
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        root.addView(emailInput);

        EditText passwordInput = buatInputLogin(
                "Password",
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        passwordInput.setTransformationMethod(
                PasswordTransformationMethod.getInstance()
        );

        root.addView(passwordInput);

        Button masuk = new Button(this);
        masuk.setText("🔐 MASUK");
        masuk.setTextSize(17);
        masuk.setAllCaps(false);

        LinearLayout.LayoutParams masukParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        masukParams.setMargins(0, 20, 0, 10);

        root.addView(masuk, masukParams);

        TextView info = new TextView(this);
        info.setText(
                "Gunakan email dan password akun Firebase RR MOTOR."
        );
        info.setTextSize(14);
        info.setGravity(Gravity.CENTER);
        info.setPadding(10, 20, 10, 10);

        root.addView(info);

        masuk.setOnClickListener(v -> {

            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (email.isEmpty()) {
                emailInput.setError("Email wajib diisi");
                emailInput.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                passwordInput.setError("Password wajib diisi");
                passwordInput.requestFocus();
                return;
            }

            masuk.setEnabled(false);
            masuk.setText("⏳ MEMASUKKAN...");

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        masuk.setEnabled(true);
                        masuk.setText("🔐 MASUK");

                        if (task.isSuccessful()) {

                            Toast.makeText(
                                    this,
                                    "Login berhasil 👍",
                                    Toast.LENGTH_SHORT
                            ).show();

                            tampilkanMenuUtama();
                            migrasiRiwayatLokalKeCloud();

                        } else {

                            String pesan = "Login gagal";

                            if (task.getException() != null &&
                                    task.getException().getMessage() != null) {
                                pesan = task.getException().getMessage();
                            }

                            Toast.makeText(
                                    this,
                                    pesan,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        setContentView(scroll);
    }

    private EditText buatInputLogin(String hint, int inputType) {

        EditText input = new EditText(this);

        input.setHint(hint);
        input.setTextSize(17);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setPadding(20, 15, 20, 15);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 10, 0, 10);

        input.setLayoutParams(params);

        return input;
    }

    // ============================================================
    // MENU UTAMA
    // ============================================================

    private void tampilkanMenuUtama() {

        scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 30);

        scrollView.addView(root);

        TextView judul = new TextView(this);
        judul.setText("🏍️ RR MOTOR NOTA");
        judul.setTextSize(25);
        judul.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        judul.setGravity(Gravity.CENTER);
        judul.setPadding(0, 10, 0, 20);

        root.addView(judul);

        namaInput = buatInput("Nama Pelanggan *");
        root.addView(namaInput);

        LinearLayout waLayout = new LinearLayout(this);
        waLayout.setOrientation(LinearLayout.HORIZONTAL);
        waLayout.setGravity(Gravity.CENTER_VERTICAL);

        waInput = buatInput("Nomor WhatsApp *");

        LinearLayout.LayoutParams waInputParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        waLayout.addView(waInput, waInputParams);

        Button kontakButton = new Button(this);
        kontakButton.setText("📱 KONTAK");
        kontakButton.setAllCaps(false);

        waLayout.addView(kontakButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(waLayout);

        kontakButton.setOnClickListener(v -> pilihKontak());

        tanggalInput = buatInput("Tanggal Nota *");
        tanggalInput.setFocusable(false);
        tanggalInput.setClickable(true);
        root.addView(tanggalInput);

        tanggalInput.setOnClickListener(v -> pilihTanggal());

        motorInput = buatInput("Jenis Motor (opsional)");
        root.addView(motorInput);

        dpInput = buatInput("DP / Uang Muka (opsional)");
        dpInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        root.addView(dpInput);

        TextView statusLabel = new TextView(this);
        statusLabel.setText("Status Pembayaran");
        statusLabel.setTextSize(16);
        statusLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusLabel.setPadding(5, 15, 5, 5);

        root.addView(statusLabel);

        statusSpinner = new Spinner(this);

        String[] statusList = {
                "BELUM LUNAS",
                "LUNAS"
        };

        ArrayAdapter<String> statusAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        statusList
                );

        statusAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        statusSpinner.setAdapter(statusAdapter);

        root.addView(statusSpinner);

        statusSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        statusBayar = statusList[position];
                        hitungTotal();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );

        TextView itemTitle = new TextView(this);
        itemTitle.setText("🧾 BARANG / JASA");
        itemTitle.setTextSize(19);
        itemTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        itemTitle.setPadding(5, 25, 5, 10);

        root.addView(itemTitle);

        itemContainer = new LinearLayout(this);
        itemContainer.setOrientation(LinearLayout.VERTICAL);

        root.addView(itemContainer);

        Button tambahItem = new Button(this);
        tambahItem.setText("➕ TAMBAH BARANG / JASA");
        tambahItem.setAllCaps(false);

        root.addView(tambahItem);

        tambahItem.setOnClickListener(v -> tambahBarisItem());

        totalText = new TextView(this);
        totalText.setTextSize(19);
        totalText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        totalText.setPadding(5, 20, 5, 5);

        root.addView(totalText);

        sisaText = new TextView(this);
        sisaText.setTextSize(18);
        sisaText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sisaText.setPadding(5, 5, 5, 5);

        root.addView(sisaText);

        statusText = new TextView(this);
        statusText.setTextSize(18);
        statusText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusText.setPadding(5, 5, 5, 15);

        root.addView(statusText);

        Button simpanButton = new Button(this);
        simpanButton.setText("💾 SIMPAN NOTA");
        simpanButton.setTextSize(17);
        simpanButton.setAllCaps(false);

        root.addView(simpanButton);

        simpanButton.setOnClickListener(v -> simpanNota());

        Button bluetoothButton = new Button(this);
        bluetoothButton.setText("🔵🖨️ CETAK BLUETOOTH");
        bluetoothButton.setTextSize(16);
        bluetoothButton.setAllCaps(false);

        root.addView(bluetoothButton);

        bluetoothButton.setOnClickListener(
                v -> pilihPrinterBluetooth()
        );

        Button printButton = new Button(this);
        printButton.setText("🖨️ CETAK NOTA / PDF");
        printButton.setTextSize(16);
        printButton.setAllCaps(false);

        root.addView(printButton);

        printButton.setOnClickListener(v -> cetakNota());

        Button riwayatButton = new Button(this);
        riwayatButton.setText("📋 RIWAYAT NOTA");
        riwayatButton.setTextSize(16);
        riwayatButton.setAllCaps(false);

        root.addView(riwayatButton);

        riwayatButton.setOnClickListener(
                v -> tampilkanRiwayat()
        );

        Button waButton = new Button(this);
        waButton.setText("💬 KIRIM VIA WHATSAPP");
        waButton.setTextSize(16);
        waButton.setAllCaps(false);

        root.addView(waButton);

        waButton.setOnClickListener(
                v -> kirimWhatsApp()
        );

        TextView garis = new TextView(this);
        garis.setText("────────────────────────");
        garis.setGravity(Gravity.CENTER);
        garis.setPadding(0, 25, 0, 5);

        root.addView(garis);

        Button keluarButton = new Button(this);
        keluarButton.setText("🚪 KELUAR AKUN");
        keluarButton.setAllCaps(false);

        root.addView(keluarButton);

        keluarButton.setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("Keluar Akun")
                    .setMessage("Apakah Anda ingin keluar dari akun?")
                    .setNegativeButton("BATAL", null)
                    .setPositiveButton("KELUAR",
                            (dialog, which) -> {

                                mAuth.signOut();
                                tampilkanLogin();
                            })
                    .show();
        });

        tambahBarisItem();

        tanggalInput.setText(
                new SimpleDateFormat(
                        "dd-MM-yyyy",
                        Locale.getDefault()
                ).format(new Date())
        );

        hitungTotal();

        setContentView(scrollView);
    }

    // ============================================================
    // INPUT
    // ============================================================

    private EditText buatInput(String hint) {

        EditText input = new EditText(this);

        input.setHint(hint);
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setPadding(15, 12, 15, 12);

        input.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        input.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (hasFocus) {
                        pastikanInputTerlihat(v);
                    }
                }
        );

        return input;
    }

    private void pastikanInputTerlihat(View view) {

        if (scrollView == null) {
            return;
        }

        scrollView.postDelayed(
                () -> scrollView.smoothScrollTo(
                        0,
                        Math.max(
                                0,
                                view.getTop() -
                                        scrollView.getHeight() / 3
                        )
                ),
                150
        );
    }

    // ============================================================
    // KONTAK
    // ============================================================

    private void pilihKontak() {

        try {

            Intent intent = new Intent(
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
                    "Kontak tidak tersedia",
                    Toast.LENGTH_SHORT
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

        if (requestCode == REQUEST_PILIH_KONTAK &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {

            Uri uri = data.getData();

            Cursor cursor = null;

            try {

                cursor = getContentResolver().query(
                        uri,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        },
                        null,
                        null,
                        null
                );

                if (cursor != null &&
                        cursor.moveToFirst()) {

                    String nomor = cursor.getString(0);

                    if (waInput != null) {
                        waInput.setText(nomor);
                    }
                }

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Gagal mengambil nomor kontak",
                        Toast.LENGTH_SHORT
                ).show();

            } finally {

                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    // ============================================================
    // TANGGAL
    // ============================================================

    private void pilihTanggal() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            String tanggal =
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d-%02d-%04d",
                                            dayOfMonth,
                                            month + 1,
                                            year
                                    );

                            tanggalInput.setText(tanggal);
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

        dialog.show();
    }

    // ============================================================
    // ITEM
    // ============================================================

    private void tambahBarisItem() {

        LinearLayout baris = new LinearLayout(this);
        baris.setOrientation(LinearLayout.VERTICAL);
        baris.setPadding(0, 10, 0, 10);

        EditText nama = buatInput("Nama barang / jasa");

        EditText jumlah = buatInput("Jumlah");
        jumlah.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        EditText harga = buatInput("Harga satuan");
        harga.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        Button hapus = new Button(this);
        hapus.setText("🗑️ HAPUS ITEM");
        hapus.setAllCaps(false);

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

            int index = namaBarang.indexOf(nama);

            if (index >= 0) {

                namaBarang.remove(index);
                jumlahBarang.remove(index);
                hargaBarang.remove(index);
            }

            itemContainer.removeView(baris);

            hitungTotal();
        });

        hitungTotal();
    }

    private void pasangListenerHitung(EditText input) {

        input.addTextChangedListener(
                new android.text.TextWatcher() {

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
                            android.text.Editable s) {
                    }
                }
        );
    }

    private long angka(EditText input) {

        if (input == null ||
                input.getText() == null) {
            return 0;
        }

        String teks =
                input.getText().toString()
                        .replace(".", "")
                        .replace(",", "")
                        .trim();

        if (teks.isEmpty()) {
            return 0;
        }

        try {
            return Long.parseLong(teks);
        } catch (Exception e) {
            return 0;
        }
    }

    // ============================================================
    // HITUNG TOTAL
    // ============================================================

    private long hitungTotalTanpaStatus() {

        long total = 0;

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            long jumlah =
                    angka(jumlahBarang.get(i));

            long harga =
                    angka(hargaBarang.get(i));

            total += jumlah * harga;
        }

        return total;
    }

    private long hitungDP() {

        return angka(dpInput);
    }

    private void hitungTotal() {

        if (totalText == null ||
                sisaText == null ||
                statusText == null) {
            return;
        }

        long total = hitungTotalTanpaStatus();
        long dp = hitungDP();

        long sisa = total - dp;

        if (sisa < 0) {
            sisa = 0;
        }

        totalText.setText(
                "TOTAL: " + formatRupiah(total)
        );

        sisaText.setText(
                "SISA: " + formatRupiah(sisa)
        );

        if ("LUNAS".equals(statusBayar)) {

            statusText.setText(
                    "STATUS: LUNAS"
            );

        } else {

            statusText.setText(
                    "STATUS: BELUM LUNAS"
            );
        }
    }

    private String formatRupiah(long angka) {

        NumberFormat nf =
                NumberFormat.getNumberInstance(
                        new Locale("id", "ID")
                );

        return "Rp " + nf.format(angka);
    }

    // ============================================================
    // SIMPAN NOTA
    // ============================================================

    private void simpanNota() {

        if (!validasiNota()) {
            return;
        }

        long timestamp;

        if (editingTimestamp > 0) {
            timestamp = editingTimestamp;
        } else {
            timestamp = System.currentTimeMillis();
        }

        String dataLokal =
                buatDataNota(timestamp);

        simpanLokal(dataLokal);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Anda belum login Firebase",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Map<String, Object> data =
                buatDataFirestore(timestamp);

        String uid = user.getUid();

        db.collection("users")
                .document(uid)
                .collection("notas")
                .document(String.valueOf(timestamp))
                .set(data)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Nota tersimpan di Firebase ☁️",
                            Toast.LENGTH_LONG
                    ).show();

                    editingTimestamp = 0;

                    bersihkanForm();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Gagal menyimpan ke Firebase: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private boolean validasiNota() {

        if (namaInput == null ||
                waInput == null ||
                tanggalInput == null) {
            return false;
        }

        if (namaInput.getText().toString().trim().isEmpty()) {

            namaInput.setError(
                    "Nama pelanggan wajib diisi"
            );

            namaInput.requestFocus();

            return false;
        }

        if (waInput.getText().toString().trim().isEmpty()) {

            waInput.setError(
                    "Nomor WhatsApp wajib diisi"
            );

            waInput.requestFocus();

            return false;
        }

        if (tanggalInput.getText().toString().trim().isEmpty()) {

            tanggalInput.setError(
                    "Tanggal nota wajib diisi"
            );

            return false;
        }

        boolean adaItem = false;

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            String nama =
                    namaBarang.get(i)
                            .getText()
                            .toString()
                            .trim();

            if (!nama.isEmpty()) {
                adaItem = true;
                break;
            }
        }

        if (!adaItem) {

            Toast.makeText(
                    this,
                    "Tambahkan minimal satu barang/jasa",
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        return true;
    }

    private Map<String, Object> buatDataFirestore(
            long timestamp) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "timestamp",
                timestamp
        );

        data.put(
                "nama",
                namaInput.getText().toString().trim()
        );

        data.put(
                "wa",
                waInput.getText().toString().trim()
        );

        data.put(
                "tanggal",
                tanggalInput.getText().toString().trim()
        );

        data.put(
                "motor",
                motorInput.getText().toString().trim()
        );

        long dp = hitungDP();
        long total = hitungTotalTanpaStatus();

        long sisa = total - dp;

        if (sisa < 0) {
            sisa = 0;
        }

        data.put("dp", dp);
        data.put("total", total);
        data.put("sisa", sisa);
        data.put("status", statusBayar);

        ArrayList<Map<String, Object>> items =
                new ArrayList<>();

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            String nama =
                    namaBarang.get(i)
                            .getText()
                            .toString()
                            .trim();

            if (nama.isEmpty()) {
                continue;
            }

            Map<String, Object> item =
                    new HashMap<>();

            item.put(
                    "nama",
                    nama
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

        data.put("items", items);

        return data;
    }

    private String buatDataNota(long timestamp) {

        StringBuilder itemData =
                new StringBuilder();

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            String nama =
                    namaBarang.get(i)
                            .getText()
                            .toString()
                            .trim();

            if (nama.isEmpty()) {
                continue;
            }

            if (itemData.length() > 0) {
                itemData.append(";");
            }

            itemData.append(
                    encode(nama)
            );

            itemData.append("~");

            itemData.append(
                    angka(jumlahBarang.get(i))
            );

            itemData.append("~");

            itemData.append(
                    angka(hargaBarang.get(i))
            );
        }

        return timestamp +
                "|" +
                encode(
                        namaInput.getText()
                                .toString()
                                .trim()
                ) +
                "|" +
                encode(
                        waInput.getText()
                                .toString()
                                .trim()
                ) +
                "|" +
                encode(
                        tanggalInput.getText()
                                .toString()
                                .trim()
                ) +
                "|" +
                encode(
                        motorInput.getText()
                                .toString()
                                .trim()
                ) +
                "|" +
                hitungDP() +
                "|" +
                encode(statusBayar) +
                "|" +
                itemData;
    }

    private void simpanLokal(String dataBaru) {

        SharedPreferences pref =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String lama =
                pref.getString(
                        KEY_HISTORY,
                        ""
                );

        StringBuilder hasil =
                new StringBuilder();

        boolean diganti = false;

        if (lama != null &&
                !lama.isEmpty()) {

            String[] baris =
                    lama.split("\\n");

            for (String data : baris) {

                if (data.trim().isEmpty()) {
                    continue;
                }

                String[] p =
                        data.split("\\|", -1);

                if (p.length > 0) {

                    try {

                        long ts =
                                Long.parseLong(
                                        p[0]
                                );

                        String[] baru =
                                dataBaru.split(
                                        "\\|",
                                        -1
                                );

                        long tsBaru =
                                Long.parseLong(
                                        baru[0]
                                );

                        if (ts == tsBaru) {

                            if (hasil.length() > 0) {
                                hasil.append("\n");
                            }

                            hasil.append(dataBaru);

                            diganti = true;

                            continue;
                        }

                    } catch (Exception ignored) {
                    }
                }

                if (hasil.length() > 0) {
                    hasil.append("\n");
                }

                hasil.append(data);
            }
        }

        if (!diganti) {

            if (hasil.length() > 0) {
                hasil.append("\n");
            }

            hasil.append(dataBaru);
        }

        pref.edit()
                .putString(
                        KEY_HISTORY,
                        hasil.toString()
                )
                .apply();
    }

    // ============================================================
    // MIGRASI DATA LAMA KE FIREBASE
    // ============================================================

    private void migrasiRiwayatLokalKeCloud() {

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        SharedPreferences pref =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String lokal =
                pref.getString(
                        KEY_HISTORY,
                        ""
                );

        if (lokal == null ||
                lokal.trim().isEmpty()) {
            return;
        }

        String uid = user.getUid();

        String keyMigrasi =
                "CLOUD_MIGRATED_" + uid;

        boolean sudah =
                pref.getBoolean(
                        keyMigrasi,
                        false
                );

        if (sudah) {
            return;
        }

        String[] baris =
                lokal.split("\\n");

        for (String data : baris) {

            if (data.trim().isEmpty()) {
                continue;
            }

            try {

                Map<String, Object> nota =
                        parseDataLokalKeMap(data);

                if (nota == null) {
                    continue;
                }

                Object timestampObject =
                        nota.get("timestamp");

                if (timestampObject == null) {
                    continue;
                }

                long timestamp =
                        ((Number) timestampObject)
                                .longValue();

                db.collection("users")
                        .document(uid)
                        .collection("notas")
                        .document(
                                String.valueOf(timestamp)
                        )
                        .set(nota);

            } catch (Exception ignored) {
            }
        }

        pref.edit()
                .putBoolean(
                        keyMigrasi,
                        true
                )
                .apply();

        Toast.makeText(
                this,
                "Data nota lama sedang disinkronkan ☁️",
                Toast.LENGTH_LONG
        ).show();
    }

    private Map<String, Object> parseDataLokalKeMap(
            String data) {

        String[] p =
                data.split("\\|", -1);

        if (p.length < 8) {
            return null;
        }

        long timestamp =
                Long.parseLong(p[0]);

        Map<String, Object> hasil =
                new HashMap<>();

        hasil.put(
                "timestamp",
                timestamp
        );

        hasil.put(
                "nama",
                decode(p[1])
        );

        hasil.put(
                "wa",
                decode(p[2])
        );

        hasil.put(
                "tanggal",
                decode(p[3])
        );

        hasil.put(
                "motor",
                decode(p[4])
        );

        long dp = 0;

        try {
            dp = Long.parseLong(p[5]);
        } catch (Exception ignored) {
        }

        hasil.put("dp", dp);

        hasil.put(
                "status",
                decode(p[6])
        );

        ArrayList<Map<String, Object>> items =
                new ArrayList<>();

        if (p.length >= 8 &&
                !p[7].isEmpty()) {

            String[] daftar =
                    p[7].split(";");

            for (String itemData : daftar) {

                String[] item =
                        itemData.split("~", -1);

                if (item.length < 3) {
                    continue;
                }

                Map<String, Object> itemMap =
                        new HashMap<>();

                itemMap.put(
                        "nama",
                        decode(item[0])
                );

                long jumlah = 0;
                long harga = 0;

                try {
                    jumlah =
                            Long.parseLong(item[1]);
                } catch (Exception ignored) {
                }

                try {
                    harga =
                            Long.parseLong(item[2]);
                } catch (Exception ignored) {
                }

                itemMap.put(
                        "jumlah",
                        jumlah
                );

                itemMap.put(
                        "harga",
                        harga
                );

                items.add(itemMap);
            }
        }

        long total = 0;

        for (Map<String, Object> item : items) {

            long jumlah =
                    ((Number) item.get("jumlah"))
                            .longValue();

            long harga =
                    ((Number) item.get("harga"))
                            .longValue();

            total += jumlah * harga;
        }

        long sisa = total - dp;

        if (sisa < 0) {
            sisa = 0;
        }

        hasil.put("total", total);
        hasil.put("sisa", sisa);
        hasil.put("items", items);

        return hasil;
    }

    // ============================================================
    // RIWAYAT FIREBASE
    // ============================================================

    private void tampilkanRiwayat() {

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Silakan login terlebih dahulu",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ProgressDialogHelper dialog =
                new ProgressDialogHelper(
                        this,
                        "⏳ Mengambil riwayat..."
                );

        dialog.show();

        long batas =
                System.currentTimeMillis()
                        - SATU_TAHUN;

        String uid =
                user.getUid();

        db.collection("users")
                .document(uid)
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
                .addOnCompleteListener(task -> {

                    dialog.dismiss();

                    if (!task.isSuccessful()) {

                        Toast.makeText(
                                this,
                                "Gagal mengambil riwayat: " +
                                        task.getException(),
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    List<DocumentSnapshot> daftar =
                            new ArrayList<>(
                                    task.getResult()
                                            .getDocuments()
                            );

                    tampilkanDialogRiwayat(
                            daftar
                    );
                });
    }

    private void tampilkanDialogRiwayat(
            List<DocumentSnapshot> daftar) {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                15,
                10,
                15,
                10
        );

        EditText cari =
                buatInput("🔎 Cari nama / nomor WhatsApp");

        root.addView(cari);

        LinearLayout daftarView =
                new LinearLayout(this);

        daftarView.setOrientation(
                LinearLayout.VERTICAL
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(daftarView);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("📋 RIWAYAT NOTA")
                        .setView(root)
                        .setNegativeButton(
                                "TUTUP",
                                null
                        )
                        .create();

        Runnable tampilkan =
                () -> {

                    daftarView.removeAllViews();

                    String kata =
                            cari.getText()
                                    .toString()
                                    .trim()
                                    .toLowerCase(
                                            Locale.getDefault()
                                    );

                    for (DocumentSnapshot doc :
                            daftar) {

                        String nama =
                                getStringField(
                                        doc,
                                        "nama"
                                );

                        String wa =
                                getStringField(
                                        doc,
                                        "wa"
                                );

                        if (!kata.isEmpty() &&
                                !nama.toLowerCase(
                                        Locale.getDefault()
                                ).contains(kata) &&
                                !wa.toLowerCase(
                                        Locale.getDefault()
                                ).contains(kata)) {

                            continue;
                        }

                        daftarView.addView(
                                buatCardRiwayat(
                                        doc,
                                        dialog
                                )
                        );
                    }

                    if (daftarView.getChildCount() == 0) {

                        TextView kosong =
                                new TextView(this);

                        kosong.setText(
                                "Tidak ada nota ditemukan."
                        );

                        kosong.setTextSize(16);
                        kosong.setGravity(
                                Gravity.CENTER
                        );

                        kosong.setPadding(
                                20,
                                40,
                                20,
                                40
                        );

                        daftarView.addView(
                                kosong
                        );
                    }
                };

        cari.addTextChangedListener(
                new android.text.TextWatcher() {

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

                        tampilkan.run();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );

        tampilkan.run();

        dialog.show();

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams();

        params.copyFrom(
                dialog.getWindow()
                        .getAttributes()
        );

        params.width =
                (int) (
                        getResources()
                                .getDisplayMetrics()
                                .widthPixels
                                * 0.95
                );

        params.height =
                (int) (
                        getResources()
                                .getDisplayMetrics()
                                .heightPixels
                                * 0.85
                );

        dialog.getWindow()
                .setAttributes(params);
    }

    private LinearLayout buatCardRiwayat(
            DocumentSnapshot doc,
            AlertDialog dialog) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                15,
                15,
                15,
                15
        );

        TextView info =
                new TextView(this);

        String nama =
                getStringField(
                        doc,
                        "nama"
                );

        String wa =
                getStringField(
                        doc,
                        "wa"
                );

        String tanggal =
                getStringField(
                        doc,
                        "tanggal"
                );

        String motor =
                getStringField(
                        doc,
                        "motor"
                );

        long total =
                getLongField(
                        doc,
                        "total"
                );

        long dp =
                getLongField(
                        doc,
                        "dp"
                );

        long sisa =
                getLongField(
                        doc,
                        "sisa"
                );

        String status =
                getStringField(
                        doc,
                        "status"
                );

        StringBuilder teks =
                new StringBuilder();

        teks.append("👤 ")
                .append(nama)
                .append("\n");

        teks.append("📱 ")
                .append(wa)
                .append("\n");

        teks.append("📅 ")
                .append(tanggal)
                .append("\n");

        if (!motor.isEmpty()) {

            teks.append("🏍️ ")
                    .append(motor)
                    .append("\n");
        }

        teks.append("💰 Total: ")
                .append(formatRupiah(total))
                .append("\n");

        teks.append("💵 DP: ")
                .append(formatRupiah(dp))
                .append("\n");

        teks.append("💳 Sisa: ")
                .append(formatRupiah(sisa))
                .append("\n");

        teks.append("📌 Status: ")
                .append(status);

        info.setText(
                teks.toString()
        );

        info.setTextSize(15);

        card.addView(info);

        LinearLayout tombol =
                new LinearLayout(this);

        tombol.setOrientation(
                LinearLayout.HORIZONTAL
        );

        tombol.setGravity(
                Gravity.CENTER
        );

        Button edit =
                new Button(this);

        edit.setText("✏️ EDIT");
        edit.setAllCaps(false);

        Button cetak =
                new Button(this);

        cetak.setText("🔵🖨️");
        cetak.setAllCaps(false);

        Button waButton =
                new Button(this);

        waButton.setText("💬 WA");
        waButton.setAllCaps(false);

        Button hapus =
                new Button(this);

        hapus.setText("🗑️");
        hapus.setAllCaps(false);

        tombol.addView(edit);
        tombol.addView(cetak);
        tombol.addView(waButton);
        tombol.addView(hapus);

        card.addView(tombol);

        edit.setOnClickListener(v -> {

            dialog.dismiss();

            muatDataNota(
                    doc
            );
        });

        cetak.setOnClickListener(v -> {

            cetakNotaFirestoreBluetooth(
                    doc
            );
        });

        waButton.setOnClickListener(v -> {

            kirimWhatsAppFirestore(
                    doc
            );
        });

        hapus.setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("Hapus Nota")
                    .setMessage(
                            "Hapus nota " +
                                    nama +
                                    "?"
                    )
                    .setNegativeButton(
                            "BATAL",
                            null
                    )
                    .setPositiveButton(
                            "HAPUS",
                            (d, which) -> {

                                hapusNotaFirestore(
                                        doc
                                );
                            })
                    .show();
        });

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                0,
                10,
                0,
                10
        );

        card.setLayoutParams(params);

        return card;
    }

    private String getStringField(
            DocumentSnapshot doc,
            String field) {

        String value =
                doc.getString(field);

        return value == null
                ? ""
                : value;
    }

    private long getLongField(
            DocumentSnapshot doc,
            String field) {

        Number number =
                doc.getLong(field);

        return number == null
                ? 0
                : number.longValue();
    }

    // ============================================================
    // EDIT NOTA
    // ============================================================

    private void muatDataNota(
            DocumentSnapshot doc) {

        tampilkanMenuUtama();

        long timestamp =
                getLongField(
                        doc,
                        "timestamp"
                );

        editingTimestamp =
                timestamp;

        namaInput.setText(
                getStringField(
                        doc,
                        "nama"
                )
        );

        waInput.setText(
                getStringField(
                        doc,
                        "wa"
                )
        );

        tanggalInput.setText(
                getStringField(
                        doc,
                        "tanggal"
                )
        );

        motorInput.setText(
                getStringField(
                        doc,
                        "motor"
                )
        );

        dpInput.setText(
                String.valueOf(
                        getLongField(
                                doc,
                                "dp"
                        )
                )
        );

        String status =
                getStringField(
                        doc,
                        "status"
                );

        statusBayar = status;

        if ("LUNAS".equals(status)) {
            statusSpinner.setSelection(1);
        } else {
            statusSpinner.setSelection(0);
        }

        itemContainer.removeAllViews();

        namaBarang.clear();
        jumlahBarang.clear();
        hargaBarang.clear();

        List<Map<String, Object>> items =
                (List<Map<String, Object>>)
                        doc.get("items");

        if (items != null &&
                !items.isEmpty()) {

            for (Map<String, Object> item :
                    items) {

                tambahBarisItem();

                int index =
                        namaBarang.size() - 1;

                String nama =
                        item.get("nama") == null
                                ? ""
                                : String.valueOf(
                                        item.get("nama")
                                );

                long jumlah =
                        getMapLong(
                                item,
                                "jumlah"
                        );

                long harga =
                        getMapLong(
                                item,
                                "harga"
                        );

                namaBarang.get(index)
                        .setText(nama);

                jumlahBarang.get(index)
                        .setText(
                                String.valueOf(
                                        jumlah
                                )
                        );

                hargaBarang.get(index)
                        .setText(
                                String.valueOf(
                                        harga
                                )
                        );
            }

        } else {

            tambahBarisItem();
        }

        hitungTotal();

        Toast.makeText(
                this,
                "Mode edit nota ✏️",
                Toast.LENGTH_SHORT
        ).show();
    }

    private long getMapLong(
            Map<String, Object> map,
            String key) {

        Object value =
                map.get(key);

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

    private void hapusNotaFirestore(
            DocumentSnapshot doc) {

        FirebaseUser user =
                mAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("notas")
                .document(doc.getId())
                .delete()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Nota berhasil dihapus 🗑️",
                            Toast.LENGTH_SHORT
                    ).show();

                    hapusNotaLokal(
                            getLongField(
                                    doc,
                                    "timestamp"
                            )
                    );

                    tampilkanRiwayat();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Gagal menghapus: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void hapusNotaLokal(
            long timestamp) {

        SharedPreferences pref =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String lama =
                pref.getString(
                        KEY_HISTORY,
                        ""
                );

        if (lama == null ||
                lama.isEmpty()) {
            return;
        }

        StringBuilder hasil =
                new StringBuilder();

        String[] baris =
                lama.split("\\n");

        for (String data : baris) {

            try {

                String[] p =
                        data.split("\\|", -1);

                long ts =
                        Long.parseLong(p[0]);

                if (ts == timestamp) {
                    continue;
                }

            } catch (Exception ignored) {
            }

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
    }

    // ============================================================
    // WHATSAPP
    // ============================================================

    private void kirimWhatsApp() {

        if (!validasiNota()) {
            return;
        }

        String nomor =
                normalisasiNomor(
                        waInput.getText()
                                .toString()
                );

        String pesan =
                buatTeksNotaWhatsApp();

        bukaWhatsApp(
                nomor,
                pesan
        );
    }

    private void kirimWhatsAppFirestore(
            DocumentSnapshot doc) {

        String nomor =
                normalisasiNomor(
                        getStringField(
                                doc,
                                "wa"
                        )
                );

        String pesan =
                buatTeksNotaWhatsAppFirestore(
                        doc
                );

        bukaWhatsApp(
                nomor,
                pesan
        );
    }

    private String normalisasiNomor(
            String nomor) {

        nomor =
                nomor.replace(
                        " ",
                        ""
                )
                .replace(
                        "-",
                        ""
                )
                .replace(
                        "+",
                        ""
                );

        if (nomor.startsWith("0")) {

            nomor =
                    "62" +
                            nomor.substring(1);
        }

        return nomor;
    }

    private void bukaWhatsApp(
            String nomor,
            String pesan) {

        try {

            String url =
                    "https://wa.me/" +
                            nomor +
                            "?text=" +
                            URLEncoder.encode(
                                    pesan,
                                    "UTF-8"
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
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String buatTeksNotaWhatsApp() {

        StringBuilder teks =
                new StringBuilder();

        teks.append("🏍️ RR MOTOR\n");
        teks.append("====================\n");

        teks.append(
                "Nama: "
        ).append(
                namaInput.getText()
                        .toString()
                        .trim()
        ).append("\n");

        teks.append(
                "Tanggal: "
        ).append(
                tanggalInput.getText()
                        .toString()
                        .trim()
        ).append("\n");

        String motor =
                motorInput.getText()
                        .toString()
                        .trim();

        if (!motor.isEmpty()) {

            teks.append(
                    "Motor: "
            ).append(
                    motor
            ).append("\n");
        }

        teks.append("\n");

        for (int i = 0;
             i < namaBarang.size();
             i++) {

            String nama =
                    namaBarang.get(i)
                            .getText()
                            .toString()
                            .trim();

            if (nama.isEmpty()) {
                continue;
            }

            long jumlah =
                    angka(
                            jumlahBarang.get(i)
                    );

            long harga =
                    angka(
                            hargaBarang.get(i)
                    );

            teks.append(
                    nama
            ).append("\n");

            teks.append(
                    jumlah
            ).append(" x ")
                    .append(
                            formatRupiah(harga)
                    )
                    .append(" = ")
                    .append(
                            formatRupiah(
                                    jumlah * harga
                            )
                    )
                    .append("\n");
        }

        long total =
                hitungTotalTanpaStatus();

        long dp =
                hitungDP();

        long sisa =
                total - dp;

        if (sisa < 0) {
            sisa = 0;
        }

        teks.append("\n");
        teks.append(
                "TOTAL: "
        ).append(
                formatRupiah(total)
        ).append("\n");

        teks.append(
                "DP: "
        ).append(
                formatRupiah(dp)
        ).append("\n");

        teks.append(
                "SISA: "
        ).append(
                formatRupiah(sisa)
        ).append("\n");

        teks.append(
                "STATUS: "
        ).append(
                statusBayar
        ).append("\n");

        teks.append("\n");
        teks.append("Terima kasih 🙏\n");
        teks.append("RR MOTOR");

        return teks.toString();
    }

    private String buatTeksNotaWhatsAppFirestore(
            DocumentSnapshot doc) {

        StringBuilder teks =
                new StringBuilder();

        String nama =
                getStringField(
                        doc,
                        "nama"
                );

        String tanggal =
                getStringField(
                        doc,
                        "tanggal"
                );

        String motor =
                getStringField(
                        doc,
                        "motor"
                );

        long total =
                getLongField(
                        doc,
                        "total"
                );

        long dp =
                getLongField(
                        doc,
                        "dp"
                );

        long sisa =
                getLongField(
                        doc,
                        "sisa"
                );

        String status =
                getStringField(
                        doc,
                        "status"
                );

        teks.append("🏍️ RR MOTOR\n");
        teks.append("====================\n");
        teks.append("Nama: ")
                .append(nama)
                .append("\n");

        teks.append("Tanggal: ")
                .append(tanggal)
                .append("\n");

        if (!motor.isEmpty()) {

            teks.append("Motor: ")
                    .append(motor)
                    .append("\n");
        }

        teks.append("\n");

        List<Map<String, Object>> items =
                (List<Map<String, Object>>)
                        doc.get("items");

        if (items != null) {

            for (Map<String, Object> item :
                    items) {

                String namaItem =
                        item.get("nama") == null
                                ? ""
                                : String.valueOf(
                                        item.get("nama")
                                );

                long jumlah =
                        getMapLong(
                                item,
                                "jumlah"
                        );

                long harga =
                        getMapLong(
                                item,
                                "harga"
                        );

                teks.append(
                        namaItem
                ).append("\n");

                teks.append(
                        jumlah
                ).append(" x ")
                        .append(
                                formatRupiah(harga)
                        )
                        .append(" = ")
                        .append(
                                formatRupiah(
                                        jumlah * harga
                                )
                        )
                        .append("\n");
            }
        }

        teks.append("\n");

        teks.append("TOTAL: ")
                .append(
                        formatRupiah(total)
                )
                .append("\n");

        teks.append("DP: ")
                .append(
                        formatRupiah(dp)
                )
                .append("\n");

        teks.append("SISA: ")
                .append(
                        formatRupiah(sisa)
                )
                .append("\n");

        teks.append("STATUS: ")
                .append(status)
                .append("\n");

        teks.append("\n");
        teks.append("Terima kasih 🙏\n");
        teks.append("RR MOTOR");

        return teks.toString();
    }

    // ============================================================
    // CETAK PDF
    // ============================================================

    private void cetakNota() {

        if (!validasiNota()) {
            return;
        }

        final String teks =
                buatTeksNota();

        PrintManager printManager =
                (PrintManager)
                        getSystemService(
                                PRINT_SERVICE
                        );

        printManager.print(
                "Nota_RR_MOTOR",
                new NotaPdfAdapter(
                        this,
                        teks
                ),
                new PrintAttributes.Builder()
                        .setMediaSize(
                                PrintAttributes.MediaSize
                                        .NA_INDEX_80MM
                        )
                        .setMinMargins(
                                PrintAttributes.Margins.NO_MARGINS
                        )
                        .build()
        );
    }

    private String buatTeksNota() {

        return buatTeksNotaWhatsApp();
    }

    // ============================================================
    // CETAK BLUETOOTH
    // ============================================================

    private void pilihPrinterBluetooth() {

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

        if (manager != null) {

            bluetoothAdapter =
                    manager.getAdapter();
        }

        if (bluetoothAdapter == null) {

            Toast.makeText(
                    this,
                    "Bluetooth tidak tersedia",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!bluetoothAdapter.isEnabled()) {

            try {

                Intent intent =
                        new Intent(
                                BluetoothAdapter.ACTION_REQUEST_ENABLE
                        );

                startActivityForResult(
                        intent,
                        REQUEST_BLUETOOTH
                );

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Silakan aktifkan Bluetooth",
                        Toast.LENGTH_SHORT
                ).show();
            }

            return;
        }

        Set<BluetoothDevice> bondedDevices;

        try {

            bondedDevices =
                    bluetoothAdapter
                            .getBondedDevices();

        } catch (SecurityException e) {

            Toast.makeText(
                    this,
                    "Izin Bluetooth belum diberikan",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (bondedDevices == null ||
                bondedDevices.isEmpty()) {

            Toast.makeText(
                    this,
                    "Belum ada printer Bluetooth yang dipasangkan.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        ArrayList<BluetoothDevice> devices =
                new ArrayList<>(
                        bondedDevices
                );

        ArrayList<String> namaPrinter =
                new ArrayList<>();

        for (BluetoothDevice device :
                devices) {

            String nama;

            try {
                nama = device.getName();
            } catch (SecurityException e) {
                nama = "Printer Bluetooth";
            }

            if (nama == null ||
                    nama.trim().isEmpty()) {

                nama = "Printer Bluetooth";
            }

            namaPrinter.add(nama);
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih Printer")
                .setItems(
                        namaPrinter.toArray(
                                new String[0]
                        ),
                        (dialog, which) -> {

                            BluetoothDevice device =
                                    devices.get(which);

                            cetakKePrinter(
                                    device,
                                    buatTeksNota()
                            );
                        }
                )
                .setNegativeButton(
                        "BATAL",
                        null
                )
                .show();
    }

    private void cetakNotaFirestoreBluetooth(
            DocumentSnapshot doc) {

        pilihPrinterBluetoothFirestore(doc);
    }

    private void pilihPrinterBluetoothFirestore(
            DocumentSnapshot doc) {

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

        if (manager != null) {
            bluetoothAdapter =
                    manager.getAdapter();
        }

        if (bluetoothAdapter == null) {
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {

            Toast.makeText(
                    this,
                    "Aktifkan Bluetooth terlebih dahulu",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Set<BluetoothDevice> bondedDevices;

        try {

            bondedDevices =
                    bluetoothAdapter
                            .getBondedDevices();

        } catch (SecurityException e) {

            return;
        }

        if (bondedDevices == null ||
                bondedDevices.isEmpty()) {

            Toast.makeText(
                    this,
                    "Belum ada printer yang dipasangkan",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ArrayList<BluetoothDevice> devices =
                new ArrayList<>(
                        bondedDevices
                );

        ArrayList<String> names =
                new ArrayList<>();

        for (BluetoothDevice device :
                devices) {

            String name;

            try {
                name = device.getName();
            } catch (Exception e) {
                name = "Printer Bluetooth";
            }

            if (name == null) {
                name = "Printer Bluetooth";
            }

            names.add(name);
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih Printer")
                .setItems(
                        names.toArray(
                                new String[0]
                        ),
                        (dialog, which) -> {

                            cetakKePrinter(
                                    devices.get(which),
                                    buatTeksNotaWhatsAppFirestore(
                                            doc
                                    )
                            );
                        }
                )
                .setNegativeButton(
                        "BATAL",
                        null
                )
                .show();
    }

    private void cetakKePrinter(
            BluetoothDevice device,
            String teks) {

        Toast.makeText(
                this,
                "Menghubungkan ke printer...",
                Toast.LENGTH_SHORT
        ).show();

        new Thread(() -> {

            BluetoothSocket socket = null;

            try {

                UUID uuid =
                        UUID.fromString(
                                "00001101-0000-1000-8000-00805F9B34FB"
                        );

                if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.S) {

                    if (checkSelfPermission(
                            Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED) {

                        runOnUiThread(() ->
                                Toast.makeText(
                                        this,
                                        "Izin Bluetooth belum diberikan",
                                        Toast.LENGTH_SHORT
                                ).show()
                        );

                        return;
                    }
                }

                socket =
                        device.createRfcommSocketToServiceRecord(
                                uuid
                        );

                if (bluetoothAdapter != null) {
                    bluetoothAdapter.cancelDiscovery();
                }

                socket.connect();

                OutputStream output =
                        socket.getOutputStream();

                output.write(
                        new byte[]{
                                0x1B,
                                0x40
                        }
                );

                output.write(
                        teks.getBytes(
                                "GBK"
                        )
                );

                output.write(
                        new byte[]{
                                0x0A,
                                0x0A,
                                0x0A
                        }
                );

                output.write(
                        new byte[]{
                                0x1D,
                                0x56,
                                0x00
                        }
                );

                output.flush();

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Nota berhasil dicetak 🔵🖨️",
                                Toast.LENGTH_SHORT
                        ).show()
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Gagal mencetak: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );

            } finally {

                if (socket != null) {

                    try {
                        socket.close();
                    } catch (Exception ignored) {
                    }
                }
            }

        }).start();
    }

    // ============================================================
    // BERSIHKAN FORM
    // ============================================================

    private void bersihkanForm() {

        namaInput.setText("");
        waInput.setText("");
        motorInput.setText("");
        dpInput.setText("");

        tanggalInput.setText(
                new SimpleDateFormat(
                        "dd-MM-yyyy",
                        Locale.getDefault()
                ).format(new Date())
        );

        statusBayar =
                "BELUM LUNAS";

        statusSpinner.setSelection(0);

        itemContainer.removeAllViews();

        namaBarang.clear();
        jumlahBarang.clear();
        hargaBarang.clear();

        tambahBarisItem();

        hitungTotal();

        scrollView.post(
                () -> scrollView.fullScroll(
                        ScrollView.FOCUS_UP
                )
        );
    }

    // ============================================================
    // HAPUS DATA LOKAL LEBIH DARI 1 TAHUN
    // ============================================================

    private void bersihkanRiwayatLama() {

        SharedPreferences pref =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String history =
                pref.getString(
                        KEY_HISTORY,
                        ""
                );

        if (history == null ||
                history.isEmpty()) {
            return;
        }

        long batas =
                System.currentTimeMillis()
                        - SATU_TAHUN;

        StringBuilder hasil =
                new StringBuilder();

        String[] baris =
                history.split("\\n");

        for (String data : baris) {

            if (data.trim().isEmpty()) {
                continue;
            }

            try {

                String[] p =
                        data.split("\\|", -1);

                long timestamp =
                        Long.parseLong(
                                p[0]
                        );

                if (timestamp < batas) {
                    continue;
                }

            } catch (Exception ignored) {
            }

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
    }

    // ============================================================
    // ENCODE / DECODE
    // ============================================================

    private String encode(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "%",
                        "%25"
                )
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
                        "%0A"
                )
                .replace(
                        "\r",
                        "%0D"
                );
    }

    private String decode(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "%0D",
                        "\r"
                )
                .replace(
                        "%0A",
                        "\n"
                )
                .replace(
                        "%7E",
                        "~"
                )
                .replace(
                        "%3B",
                        ";"
                )
                .replace(
                        "%7C",
                        "|"
                )
                .replace(
                        "%25",
                        "%"
                );
    }

    // ============================================================
    // PERMISSION BLUETOOTH
    // ============================================================

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

            boolean semua =
                    true;

            for (int result :
                    grantResults) {

                if (result !=
                        PackageManager.PERMISSION_GRANTED) {

                    semua = false;
                    break;
                }
            }

            if (semua) {

                Toast.makeText(
                        this,
                        "Izin Bluetooth diberikan",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    // ============================================================
    // PDF PRINT ADAPTER
    // ============================================================

    private static class NotaPdfAdapter
            extends PrintDocumentAdapter {

        private final Context context;
        private final String teks;

        NotaPdfAdapter(
                Context context,
                String teks) {

            this.context = context;
            this.teks = teks;
        }

        @Override
        public void onLayout(
                PrintAttributes oldAttributes,
                PrintAttributes newAttributes,
                android.os.CancellationSignal cancellationSignal,
                LayoutResultCallback callback,
                Bundle extras) {

            if (cancellationSignal.isCanceled()) {

                callback.onLayoutCancelled();
                return;
            }

            PrintDocumentInfo info =
                    new PrintDocumentInfo.Builder(
                            "Nota_RR_MOTOR.pdf"
                    )
                            .setContentType(
                                    PrintDocumentInfo.CONTENT_TYPE_DOCUMENT
                            )
                            .build();

            callback.onLayoutFinished(
                    info,
                    true
            );
        }

        @Override
        public void onWrite(
                android.print.PageRange[] pages,
                android.os.ParcelFileDescriptor destination,
                android.os.CancellationSignal cancellationSignal,
                WriteResultCallback callback) {

            PdfDocument pdf =
                    new PdfDocument();

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(
                            226,
                            900,
                            1
                    ).create();

            PdfDocument.Page page =
                    pdf.startPage(pageInfo);

            android.graphics.Canvas canvas =
                    page.getCanvas();

            android.graphics.Paint paint =
                    new android.graphics.Paint();

            paint.setTextSize(8);
            paint.setTypeface(
                    Typeface.MONOSPACE
            );

            String[] lines =
                    teks.split("\n");

            float y = 15;

            for (String line :
                    lines) {

                if (y > 880) {
                    break;
                }

                canvas.drawText(
                        line,
                        5,
                        y,
                        paint
                );

                y += 11;
            }

            pdf.finishPage(page);

            try {

                pdf.writeTo(
                        new java.io.FileOutputStream(
                                destination.getFileDescriptor()
                        )
                );

                callback.onWriteFinished(
                        new android.print.PageRange[]{
                                android.print.PageRange.ALL_PAGES
                        }
                );

            } catch (Exception e) {

                callback.onWriteFailed(
                        e.getMessage()
                );

            } finally {

                pdf.close();
            }
        }
    }

    // ============================================================
    // HELPER PROGRESS
    // ============================================================

    private static class ProgressDialogHelper {

        private final Activity activity;
        private final String message;
        private AlertDialog dialog;

        ProgressDialogHelper(
                Activity activity,
                String message) {

            this.activity = activity;
            this.message = message;
        }

        void show() {

            TextView text =
                    new TextView(activity);

            text.setText(message);
            text.setTextSize(17);
            text.setGravity(
                    Gravity.CENTER
            );

            text.setPadding(
                    40,
                    30,
                    40,
                    30
            );

            dialog =
                    new AlertDialog.Builder(
                            activity
                    )
                            .setView(text)
                            .setCancelable(false)
                            .create();

            dialog.show();
        }

        void dismiss() {

            if (dialog != null &&
                    dialog.isShowing()) {

                dialog.dismiss();
            }
        }
    }
}
