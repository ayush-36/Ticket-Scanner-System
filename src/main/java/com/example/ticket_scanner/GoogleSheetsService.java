package com.example.ticket_scanner;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetsService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final Sheets sheetsService;

    @Value("${sheets.spreadsheet-id}")
    private String spreadsheetId;

    public GoogleSheetsService() throws Exception {
        this.sheetsService = getSheetsService();
    }

    private Sheets getSheetsService() throws Exception {
        FileInputStream serviceAccountStream = new FileInputStream("src/main/resources/credentials.json");

        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(serviceAccountStream)
                .toBuilder()
                .setScopes(Collections.singleton("https://www.googleapis.com/auth/spreadsheets"))
                .build();


        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("College Event Scanner")
                .build();
    }

    public void logScan(String ticketId) {
        try {
            ValueRange body = new ValueRange()
                    .setValues(List.of(List.of(ticketId, LocalDateTime.now().toString())));
            sheetsService.spreadsheets().values()
                    .append(spreadsheetId, "Sheet1!A:B", body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
