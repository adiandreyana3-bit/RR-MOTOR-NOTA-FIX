package com.rrmotor.nota;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText namaInput, waInput, tanggalInput, motorInput, dpInput;
    private LinearLayout itemContainer;
    private TextView totalText, sisaText, statusText;

    private final ArrayList<EditText> namaBarang = new ArrayList<>();
    private final ArrayList<EditText> jumlahBarang = new ArrayList<>();
    private final ArrayList<EditText> hargaBarang = new ArrayList<>();

    private static final String PREF_NAME = "RR_MOTOR_NOTA";
    private static final String KEY_HISTORY = "HISTORY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);

        LinearLayout utama = new LinearLayout(this);
        utama.setOrientation(LinearLayout.VERTICAL);
        utama.setPadding(25, 25, 25, 40);

        scrollView.addView(utama);

        TextView judul = new TextView(this);
        judul.setText("🏍️ RR MOTOR NOTA");
        judul.setTextSize(28);
        judul.setTypeface(null, Typeface.BOLD);
        judul.setGravity(Gravity.CENTER);
        judul.setPadding(0, 10, 0, 25);
        utama.addView(judul);

        namaInput = buatInput("Nama pelanggan *");
        utama.addView(namaInput);

        waInput = buatInput("Nomor WhatsApp *");
        waInput.setInputType(InputType.TYPE_CLASS_PHONE);
        utama.addView(waInput);

        tanggalInput = buatInput("Tanggal nota *");
        tanggalInput.setFocusable(false);
        tanggalInput.setOnClickListener(v -> pilihTanggal());
        utama.addView(tanggalInput);

        motorInput = buatInput("Jenis motor (opsional)");
        utama.addView(motorInput);

        dpInput = buatInput("DP / Uang muka (opsional)");
        dpInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        utama.addView(dpInput);

        TextView judulItem = new TextView(this);
        judulItem.setText("🧾 DAFTAR BARANG / JASA");
        judulItem.setTextSize(20);
        judulItem.setTypeface(null, Typeface.BOLD);
        judulItem.setPadding(0, 25, 0, 10);
        utama.addView(judulItem);

        itemContainer = new LinearLayout(this);
        itemContainer.setOrientation(LinearLayout.VERTICAL);
        utama.addView(itemContainer);

        Button tambahItem = new Button(this);
        tambahItem.setText("＋ Tambah Barang / Jasa");
        tambahItem.setOnClickListener(v -> tambahBarisItem());
        utama.addView(tambahItem);

        totalText = new TextView(this);
        totalText.setTextSize(20);
        totalText.setTypeface(null, Typeface.BOLD);
        totalText.setPadding(0, 25, 0, 5);
        utama.addView(totalText);

        sisaText = new TextView(this);
        sisaText.setTextSize(20);
        sisaText.setTypeface(null, Typeface.BOLD);
        utama.addView(sisaText);

        statusText = new TextView(this);
        statusText.setTextSize(18);
        statusText.setPadding(0, 5, 0, 15);
        utama.addView(statusText);

        Button simpan = new Button(this);
        simpan.setText("💾 SIMPAN NOTA");
        simpan.setOnClickListener(v -> simpanNota());
        utama.addView(simpan);

        Button cetak = new Button(this);
        cetak.setText("🖨️ CETAK NOTA");
        cetak.setOnClickListener(v -> cetakNota());
        utama.addView(cetak);

        Button riwayat = new Button(this);
        riwayat.setText("📋 RIWAYAT NOTA");
        riwayat.setOnClickListener(v -> tampilkanRiwayat());
        utama.addView(riwayat);

        Button whatsapp = new Button(this);
        whatsapp.setText("💬 KIRIM VIA WHATSAPP");
        whatsapp.setOnClickListener(v -> kirimWhatsApp());
        utama.addView(whatsapp);

        setContentView(scrollView);

        tanggalInput.setText(
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                ).format(new Date())
        );

        tambahBarisItem();
        hitungTotal();
    }

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

    private void tambahBarisItem() {

        LinearLayout baris =
                new LinearLayout(this);

        baris.setOrientation(
                LinearLayout.VERTICAL
        );

        baris.setPadding(0, 10, 0, 10);

        EditText nama =
                buatInput("Nama barang / jasa");

        EditText jumlah =
                buatInput("Jumlah");

        EditText harga =
                buatInput("Harga satuan");

        jumlah.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        harga.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

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

        jumlah.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (!hasFocus) {
                        hitungTotal();
                    }
                }
        );

        harga.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (!hasFocus) {
                        hitungTotal();
                    }
                }
        );

        dpInput.setOnFocusChangeListener(
                (v, hasFocus) -> {

                    if (!hasFocus) {
                        hitungTotal();
                    }
                }
        );

        hapus.setOnClickListener(v -> {

            int posisi =
                    namaBarang.indexOf(nama);

            if (namaBarang.size() > 1) {

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
        });
    }

    private long angka(EditText input) {

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

            long jumlah =
                    angka(jumlahBarang.get(i));

            long harga =
                    angka(hargaBarang.get(i));

            total += jumlah * harga;
        }

        long dp = angka(dpInput);

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
                    "STATUS : BELUM LUNAS"
            );
        }

        return total;
    }

    private String formatRupiah(long angka) {

        NumberFormat format =
                NumberFormat.getNumberInstance(
                        new Locale("id", "ID")
                );

        return "Rp " + format.format(angka);
    }

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

            return;
        }

        if (wa.isEmpty()) {

            waInput.setError(
                    "Nomor WhatsApp wajib diisi"
            );

            waInput.requestFocus();

            return;
        }

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

        long waktu =
                System.currentTimeMillis();

        String data =
                waktu + "|" +
                encode(nama) + "|" +
                encode(wa) + "|" +
                encode(tanggal) + "|" +
                encode(
                        motorInput
                                .getText()
                                .toString()
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

    private String encode(String teks) {

        return teks
                .replace("|", "%7C")
                .replace(";", "%3B")
                .replace("~", "%7E")
                .replace("\n", " ");
    }

    private String decode(String teks) {

        return teks
                .replace("%7C", "|")
                .replace("%3B", ";")
                .replace("%7E", "~");
    }

    private void tampilkanRiwayat() {

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
                    .setTitle("📋 Riwayat Nota")
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
                        semua[i].split(
                                "\\|",
                                7
                        );

                if (bagian.length < 7) {
                    continue;
                }

                String nama =
                        decode(bagian[1]);

                String wa =
                        decode(bagian[2]);

                String tanggal =
                        decode(bagian[3]);

                long total = 0;

                String[] items =
                        bagian[6].split(";");

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

                tampilan.append("👤 ")
                        .append(nama)
                        .append("\n");

                tampilan.append("📅 ")
                        .append(tanggal)
                        .append("\n");

                tampilan.append("📱 ")
                        .append(wa)
                        .append("\n");

                tampilan.append("💰 ")
                        .append(
                                formatRupiah(total)
                        )
                        .append("\n");

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
                .setTitle("📋 RIWAYAT NOTA")
                .setView(scroll)
                .setPositiveButton(
                        "Tutup",
                        null
                )
                .show();
    }

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
        .append(formatRupiah(total))
        .append("*\n");

        pesan.append(
                "DP : "
        )
        .append(formatRupiah(dp))
        .append("\n");

        pesan.append(
                "SISA : "
        )
        .append(formatRupiah(sisa))
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
        )
        .append(
                "kendaraan Anda kepada *RR MOTOR*. 🙏"
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

    private String buatTeksNota(
            String nama,
            String wa,
            String tanggal,
            String motor,
            long total,
            long dp,
            long sisa) {

        StringBuilder nota =
                new StringBuilder();

        nota.append(
                "================================\n"
        );

        nota.append(
                "          RR MOTOR\n"
        );

        nota.append(
                "       NOTA SERVIS\n"
        );

        nota.append(
                "================================\n\n"
        );

        nota.append("Nama    : ")
                .append(nama)
                .append("\n");

        nota.append("WhatsApp: ")
                .append(wa)
                .append("\n");

        nota.append("Tanggal : ")
                .append(tanggal)
                .append("\n");

        if (!motor.isEmpty()) {

            nota.append("Motor   : ")
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
                    angka(jumlahBarang.get(i));

            long harga =
                    angka(hargaBarang.get(i));

            if (!barang.isEmpty()) {

                nota.append(barang)
                        .append("\n");

                nota.append(jumlah)
                        .append(" x ")
                        .append(
                                formatRupiah(harga)
                        )
                        .append(" = ")
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

        nota.append("TOTAL : ")
                .append(formatRupiah(total))
                .append("\n");

        nota.append("DP    : ")
                .append(formatRupiah(dp))
                .append("\n");

        nota.append("SISA  : ")
                .append(formatRupiah(sisa))
                .append("\n");

        nota.append(
                "--------------------------------\n"
        );

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
                "Terima kasih sudah mempercayakan\n"
        );

        nota.append(
                "kendaraan Anda kepada RR MOTOR.\n"
        );

        nota.append("\n");

        nota.append(
                "        RR MOTOR\n"
        );

        nota.append(
                "================================\n"
        );

        return nota.toString();
    }

    private void cetakNota() {

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

        if (nama.isEmpty()) {

            namaInput.setError(
                    "Nama wajib diisi"
            );

            namaInput.requestFocus();

            return;
        }

        if (wa.isEmpty()) {

            waInput.setError(
                    "Nomor WhatsApp wajib diisi"
            );

            waInput.requestFocus();

            return;
        }

        if (tanggal.isEmpty()) {

            tanggalInput.setError(
                    "Tanggal wajib diisi"
            );

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

        String isiNota =
                buatTeksNota(
                        nama,
                        wa,
                        tanggal,
                        motor,
                        total,
                        dp,
                        sisa
                );

        PrintManager printManager =
                (PrintManager)
                        getSystemService(
                                PRINT_SERVICE
                        );

        if (printManager != null) {

            String namaCetak =
                    "Nota RR MOTOR - " +
                    nama;

            PrintAttributes attributes =
                    new PrintAttributes.Builder()
                            .setMediaSize(
                                    PrintAttributes.MediaSize.ISO_A4
                            )
                            .setMinMargins(
                                    PrintAttributes.Margins.NO_MARGINS
                            )
                            .build();

            printManager.print(
                    namaCetak,
                    new NotaPrintAdapter(isiNota),
                    attributes
            );

        } else {

            Toast.makeText(
                    this,
                    "Fitur cetak tidak tersedia di HP ini",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
