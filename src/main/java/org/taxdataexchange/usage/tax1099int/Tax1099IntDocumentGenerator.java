package org.taxdataexchange.usage.tax1099int;

import irs.fdxtax640.jsonserializers.Tax1099IntSerializer;
import irs.fdxtax640.jsonserializers.TaxStatementListSerializer;
import irs.fdxtax640.map2objects.Map2Tax1099Int;
import irs.fdxtax640.obj2mappers.TaxStatementList2Map;

import org.apache.commons.lang3.time.StopWatch;
import org.taxdataexchange.core.pdfbuilders.Tax1099IntPdfBuilder;

import org.taxdataexchange.core.utils.*;
import org.taxdataexchange.core.csv.GenericCsvMapReader;

import org.openapitools.client.model.*;

import returnagnosticutils.BytesToFile;
import returnagnosticutils.FileToString;
import returnagnosticutils.Jsonizer;
import returnagnosticutils.StringToFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Read CSV file for one company and generate 1099-MISC PDFs

public class Tax1099IntDocumentGenerator {

    public static final String INPUT_DIRECTORY = "input";

    public static final String OUTPUT_DIRECTORY = "output";

    private void processOneRow(
        String companyName,
        int rowNumber,
        Map<String, String> row
    ) {

        String outputDir = String.format( "%s/%s/%06d", OUTPUT_DIRECTORY, companyName, rowNumber );

        // Save input
        {
            String asJson = Jsonizer.toJson( row );
            String outputFile = String.format( "%06d.map.json", rowNumber );
            StringToFile.writeToDirFile( asJson, outputDir, outputFile );
        }

        // Convert to object and save
        Tax1099Int obj = Map2Tax1099Int.generate( row );
        {
            String asJson = Tax1099IntSerializer.serialize( obj );
            String outputFile = String.format( "%06d.obj.json", rowNumber );
            StringToFile.writeToDirFile( asJson, outputDir, outputFile );
        }

        // Create TaxStatementList object
        TaxData taxData = new TaxData( );
        taxData.setTax1099Int( obj );
        TaxStatement taxStatement = new TaxStatement( );
        taxStatement.addFormsItem( taxData );
        TaxStatementList taxStatementList = new TaxStatementList( );
        taxStatementList.addStatementsItem( taxStatement );
        {
            String asJson = TaxStatementListSerializer.serialize( taxStatementList );
            String outputFile = String.format( "%06d.tax-statement-list.json", rowNumber );
            StringToFile.writeToDirFile( asJson, outputDir, outputFile );
        }

        // Generate PDFs
        Tax1099IntPdfBuilder pdfBuilder = new Tax1099IntPdfBuilder( );
        StringBuilder stringBuilder = new StringBuilder();
        StopWatch stopWatch = new StopWatch( );
        stopWatch.start( );
        pdfBuilder.build3( stringBuilder, stopWatch, taxStatementList );
        {
            // Issuer copy
            String fileName = String.format( "%06d.issuer.pdf", rowNumber );
            byte[] pdfBytes = pdfBuilder.getIssuerPdfBytes( );
            BytesToFile.writeToDirFile(
                pdfBytes,
                outputDir,
                fileName
            );
        }

        {
            // Print and mail copy
            byte[] pdfBytes = pdfBuilder.getPrintPdfBytes( );
            pdfBytes = PdfWatermarker.addWatermarkToPdf( pdfBytes, "Sample" );
            String fileName = String.format( "%06d.print.pdf", rowNumber );
            BytesToFile.writeToDirFile(
                pdfBytes,
                outputDir,
                fileName
            );

            byte[] pngBytes = Pdf2PngConverter.convertBytes( pdfBytes );
            String pngFileName = String.format( "%06d.print.png", rowNumber );
            BytesToFile.writeToDirFile(
                pngBytes,
                outputDir,
                pngFileName
            );
        }

        {
            // Email or download copy
            byte[] pdfBytes = pdfBuilder.getDownloadPdfBytes( );
            pdfBytes = PdfWatermarker.addWatermarkToPdf( pdfBytes, "Sample" );
            String fileName = String.format( "%06d.download.pdf", rowNumber );
            BytesToFile.writeToDirFile(
                pdfBytes,
                outputDir,
                fileName
            );

            byte[] pngBytes = Pdf2PngConverter.convertBytes( pdfBytes );
            String pngFileName = String.format( "%06d.download.png", rowNumber );
            BytesToFile.writeToDirFile(
                pngBytes,
                outputDir,
                pngFileName
            );

        }

    }

    private void processCsvForCompany(
        String companyName
    ) {

        GenericCsvMapReader csvReader = new GenericCsvMapReader( );

        // CSV file
        String csvFileName  = "Tax1099Int.csv";

        // CSV dir
        String csvDirName = String.format( "%s/%s", INPUT_DIRECTORY, companyName );

        // Read content
        String csvContent = FileUtils.readDirFile(
            csvDirName,
            csvFileName
        );

        // Convert to list of maps
        List<Map<String, String>> rows = csvReader.readStringWithCsvMapReader(
            csvContent
        );

        // Save
        String outputDirName = String.format( "%s/%s", OUTPUT_DIRECTORY, companyName );
        String outputFileName = "Tax1099Int.rows.json";
        String asJson = Jsonizer.toJson( rows );
        StringToFile.writeToDirFile( asJson, outputDirName, outputFileName );

        // Process each row
        int rowNumber = 0;
        for ( Map<String, String> row : rows ) {

            rowNumber++;
            processOneRow( companyName, rowNumber, row );

        }

    }

    public static void main(String[] args) {

        System.out.println( "Tax1099IntDocumentGenerator Begin" );

        String companyName = "company1";

        Tax1099IntDocumentGenerator generator = new Tax1099IntDocumentGenerator( );

        generator.processCsvForCompany( companyName );

        System.out.println( "Tax1099IntDocumentGenerator Done" );

    }

}
