package com.rrmotor.nota;

import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;

import java.io.FileOutputStream;
import java.io.IOException;

public class NotaPrintAdapter extends PrintDocumentAdapter {

    private final String isiNota;
    private PdfDocument pdfDocument;

    public NotaPrintAdapter(String isiNota) {
        this.isiNota = isiNota;
    }

    @Override
    public void onLayout(
            PrintAttributes oldAttributes,
            PrintAttributes newAttributes,
            CancellationSignal cancellationSignal,
            LayoutResultCallback callback,
            Bundle extras) {

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo info =
                new PrintDocumentInfo.Builder("Nota_RR_MOTOR.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build();

        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(
            PageRange[] pages,
            ParcelFileDescriptor destination,
            CancellationSignal cancellationSignal,
            WriteResultCallback callback) {

        pdfDocument = new PdfDocument();

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();

        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        android.graphics.Canvas canvas = page.getCanvas();
        android.graphics.Paint paint = new android.graphics.Paint();

        paint.setTextSize(14);

        float x = 30;
        float y = 40;

        String[] baris = isiNota.split("\n");

        for (String teks : baris) {
            if (cancellationSignal.isCanceled()) {
                pdfDocument.close();
                callback.onWriteCancelled();
                return;
            }

            canvas.drawText(teks, x, y, paint);
            y += 20;

            if (y > 820) {
                break;
            }
        }

        pdfDocument.finishPage(page);

        try {
            FileOutputStream output =
                    new FileOutputStream(destination.getFileDescriptor());

            pdfDocument.writeTo(output);
            output.close();

            callback.onWriteFinished(new PageRange[]{
                    PageRange.ALL_PAGES
            });

        } catch (IOException e) {
            callback.onWriteFailed(e.getMessage());

        } finally {
            pdfDocument.close();
            pdfDocument = null;
        }
    }
}
