package com.restaurant.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.restaurant.model.Order;
import com.restaurant.model.OrderItem;
import com.restaurant.model.Payment;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class InvoiceGenerator {

    private static final Font FONT_TITLE   = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(44, 62, 80));
    private static final Font FONT_HEADER  = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
    private static final Font FONT_NORMAL  = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(44, 62, 80));
    private static final Font FONT_BOLD    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(44, 62, 80));
    private static final Font FONT_SMALL   = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BaseColor.GRAY);
    private static final Font FONT_TOTAL   = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   new BaseColor(39, 174, 96));

    private static final BaseColor COLOR_ACCENT = new BaseColor(52, 152, 219);
    private static final BaseColor COLOR_LIGHT  = new BaseColor(236, 240, 241);

    public static String generate(Order order, Payment payment, String outputDir) throws Exception {
        String filename = outputDir + "/facture_" + order.getId() + "_" +
                System.currentTimeMillis() + ".pdf";

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(filename));
        doc.open();

        // Header
        addHeader(doc, order, payment);

        doc.add(Chunk.NEWLINE);

        // Items table
        addItemsTable(doc, order);

        doc.add(Chunk.NEWLINE);

        // Totals
        addTotals(doc, order, payment);

        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);

        // Footer
        addFooter(doc);

        doc.close();
        return filename;
    }

    private static void addHeader(Document doc, Order order, Payment payment) throws DocumentException {
        // Restaurant name
        Paragraph title = new Paragraph("🍽️  Restaurant Manager", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        doc.add(new Paragraph("Restaurant Le Delice, Yaoundé-Cameroun | Tél : +237 6 75 86 74 26", FONT_SMALL));
        doc.add(new LineSeparator(1, 100, COLOR_ACCENT, Element.ALIGN_CENTER, -5));
        doc.add(Chunk.NEWLINE);

        // Invoice info table
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        Paragraph facNo = new Paragraph("FACTURE N° " + String.format("%05d", order.getId()), FONT_BOLD);
        leftCell.addElement(facNo);
        leftCell.addElement(new Paragraph("Date : " +
                (payment.getCreatedAt() != null
                        ? payment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "-"), FONT_NORMAL));
        leftCell.addElement(new Paragraph("Table : " + order.getTableNumero(), FONT_NORMAL));
        if (order.getServeur() != null && !order.getServeur().isEmpty()) {
            leftCell.addElement(new Paragraph("Serveur : " + order.getServeur(), FONT_NORMAL));
        }
        infoTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Paragraph("Mode de paiement :", FONT_SMALL));
        rightCell.addElement(new Paragraph(payment.getMethode().getLabel(), FONT_BOLD));
        if (payment.getMontantRecu() != null) {
            rightCell.addElement(new Paragraph("Montant reçu : " +
                    String.format("%.0f FCFA", payment.getMontantRecu()), FONT_NORMAL));
            rightCell.addElement(new Paragraph("Monnaie rendue : " +
                    String.format("%.0f FCFA", payment.getMonnaie() != null ? payment.getMonnaie() : BigDecimal.ZERO), FONT_NORMAL));
        }
        infoTable.addCell(rightCell);

        doc.add(infoTable);
        doc.add(Chunk.NEWLINE);
    }

    private static void addItemsTable(Document doc, Order order) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40, 10, 20, 20});

        // Header row
        String[] headers = {"Article", "Qté", "Prix unit.", "Sous-total"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_HEADER));
            cell.setBackgroundColor(COLOR_ACCENT);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Items
        boolean alt = false;
        for (OrderItem item : order.getItems()) {
            BaseColor bg = alt ? COLOR_LIGHT : BaseColor.WHITE;

            addCell(table, item.getMenuItemNom(), FONT_NORMAL, Element.ALIGN_LEFT, bg, 6);
            addCell(table, String.valueOf(item.getQuantite()), FONT_NORMAL, Element.ALIGN_CENTER, bg, 6);
            addCell(table, String.format("%.0f FCFA", item.getPrixUnitaire()), FONT_NORMAL, Element.ALIGN_RIGHT, bg, 6);
            addCell(table, String.format("%.0f FCFA", item.getSousTotal()), FONT_BOLD, Element.ALIGN_RIGHT, bg, 6);

            alt = !alt;
        }

        doc.add(table);
    }

    private static void addTotals(Document doc, Order order, Payment payment) throws DocumentException {
        PdfPTable totTable = new PdfPTable(2);
        totTable.setWidthPercentage(50);
        totTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        BigDecimal total = order.getTotal();
        BigDecimal tva   = total.multiply(new BigDecimal("0.1925")); // TVA 19,25%
        BigDecimal ht    = total.subtract(tva);

        addTotalRow(totTable, "Sous-total HT :",  String.format("%.2f FCFA", ht),    false);
        addTotalRow(totTable, "TVA (19,25%) :",       String.format("%.2f FCFA", tva),   false);
        addTotalRow(totTable, "TOTAL TTC :",       String.format("%.0f FCFA", total), true);

        doc.add(totTable);
    }

    private static void addFooter(Document doc) throws DocumentException {
        doc.add(new LineSeparator(0.5f, 100, COLOR_ACCENT, Element.ALIGN_CENTER, -5));
        Paragraph footer = new Paragraph("Merci de votre visite ! À bientôt", FONT_SMALL);
        footer.setAlignment(Element.ALIGN_CENTER);

        Paragraph siret = new Paragraph("NIU : M092412345678A | RCCM : RC/YAO/2026/B/5678", FONT_SMALL);
        siret.setAlignment(Element.ALIGN_CENTER);
        doc.add(siret);
        doc.add(footer);
    }

    private static void addCell(PdfPTable table, String text, Font font, int align, BaseColor bg, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(bg);
        cell.setPadding(padding);
        table.addCell(cell);
    }

    private static void addTotalRow(PdfPTable table, String label, String value, boolean highlight) {
        Font f = highlight ? FONT_TOTAL : FONT_NORMAL;
        BaseColor bg = highlight ? new BaseColor(232, 255, 240) : BaseColor.WHITE;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, highlight ? FONT_TOTAL : FONT_BOLD));
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBackgroundColor(bg);
        labelCell.setPadding(6);
        labelCell.setBorder(Rectangle.TOP);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, f));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBackgroundColor(bg);
        valueCell.setPadding(6);
        valueCell.setBorder(Rectangle.TOP);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
