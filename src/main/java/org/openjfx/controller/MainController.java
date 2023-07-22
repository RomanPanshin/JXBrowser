package org.openjfx.controller;
import org.apache.pdfbox.pdmodel.PDDocument;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


import org.apache.commons.io.IOUtils;
import org.openjfx.PDFManager;

public class MainController {
    @FXML
    private TextField urlField;
    @FXML
    private Button goButton;
    @FXML
    private Button newTabButton;
    @FXML
    private TabPane tabPane;
    @FXML
    private Button saveTextButton;
    @FXML
    private Button backButton;


    private Map<Tab, Stack<String>> tabHistory = new HashMap<>();

    @FXML
    public void initialize() {
        goButton.setOnAction(this::loadUrl);
        newTabButton.setOnAction(this::newTab);
        saveTextButton.setOnAction(this::saveSelectedText);
        backButton.setOnAction(this::goBack);

        // Handle when a tab is selected.
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            WebView webView = (WebView) newTab.getContent();
            urlField.setText(webView.getEngine().getLocation());
        });

        tabPane.getTabs().addListener((ListChangeListener.Change<? extends Tab> c) -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (Tab removedTab : c.getRemoved()) {
                        tabHistory.remove(removedTab);  // remove the history stack when a tab is closed
                    }
                }
            }
        });

        // Load default URL in the first tab.
        Tab firstTab = tabPane.getTabs().get(0);
        tabHistory.put(firstTab, new Stack<>());
        loadWebsite(firstTab, "https://www.google.com");
        tabHistory.get(firstTab).push("https://www.google.com");
    }


    public void loadUrl(ActionEvent event) {
        // Get current WebView and load URL.
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        loadWebsite(currentTab, urlField.getText());
    }

    public void newTab(ActionEvent event) {
        WebView newWebView = new WebView();
        Tab newTab = new Tab("New Tab", newWebView);
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);
        loadWebsite(newTab, "https://www.google.com");
        tabHistory.put(newTab, new Stack<>());
        tabHistory.get(newTab).push("https://www.google.com");

    }

    private void loadWebsite(Tab tab, String url) {
        WebView webView = (WebView) tab.getContent();
        WebEngine webEngine = webView.getEngine();

        webEngine.locationProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            // If the new URL is a PDF, load it in the WebView.
            if (newValue.endsWith(".pdf")) {
                urlField.setText(newValue);
                loadPdf(webEngine, newValue);
            }
        });

        // Load URL.
        webEngine.load(url);
        webEngine.locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                urlField.setText(newValue);
                tab.setText(getHeaderName(webEngine, newValue));

                // Add the new URL to the history stack for this tab
                if (!tabHistory.containsKey(tab)) {
                    tabHistory.put(tab, new Stack<>());
                }
                tabHistory.get(tab).push(newValue);
            }
        });

        tab.setContent(webView);
    }




    private void loadPdf(WebEngine webEngine, String url) {
            // Load PDF.js viewer with the PDF.
            String viewerHtml = getClass().getResource("/web/viewer.html").toExternalForm();
            String viewerCss = getClass().getResource("/web/viewer.css").toExternalForm();
            webEngine.setJavaScriptEnabled(true);
            webEngine.load(viewerHtml);

            webEngine.getLoadWorker().stateProperty().addListener((obs, oldV, newV) -> {
                if (Worker.State.SUCCEEDED == newV) {
                    InputStream stream = null;
                    try {
                        PDFManager pdfManager = new PDFManager();
                        String base64 = pdfManager.getPdfAsBase64(url);;
                        webEngine.executeScript("openFileFromBase64('" + base64 + "')");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            });
            webEngine.setUserStyleSheetLocation(viewerCss);

        }


    public void saveSelectedText(ActionEvent event) {
        // Get current WebView.
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        WebView webView = (WebView) currentTab.getContent();
        WebEngine webEngine = webView.getEngine();

        // Get the selected text.
        String selectedText = (String) webEngine.executeScript("window.getSelection().toString()");

        Stack<String> history = tabHistory.get(currentTab);

        // Last visited URL
        String lastVisitedURL = history.peek();
        System.out.println(lastVisitedURL);
        if(lastVisitedURL.startsWith("file")){
            history.pop();
            lastVisitedURL = history.peek();
        }
        System.out.println(lastVisitedURL);

        // Get header name
        String headerName = getHeaderName(webEngine, lastVisitedURL);

        // Construct the format
        String saveText = "\"" + selectedText + "\" [" + headerName + "](" + lastVisitedURL + ")\n";

        // Save the selected text to a file.
        try {
            Path filePath = Paths.get("selectedText.txt");

            // Create the file if it doesn't exist.
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }

            Files.write(filePath, saveText.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private String getHeaderName(WebEngine webEngine, String url) {
        if (url.endsWith(".pdf")) {
            return getPDFHeaderName(url);
        } else {
            return (String) webEngine.executeScript("document.title");
        }
    }

    private String getPDFHeaderName(String url) {
        PDDocument document = null;
        try {
            PDFManager pdfManager = new PDFManager();
            byte[] pdfBytes = Base64.getDecoder().decode(pdfManager.getPdfAsBase64(url));
            document = PDDocument.load(pdfBytes);
            return document.getDocumentInformation().getTitle();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Return an empty string if the header name couldn't be obtained
        return "";
    }





    public Stack<String> getUrlHistoryForTab(Tab tab) {
        return tabHistory.get(tab);
    }

    public void goBack(ActionEvent event) {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        Stack<String> history = tabHistory.get(currentTab);

        if (history == null || history.size() <= 1) { // <= 1 because current page is also in history
            Alert alert = new Alert(Alert.AlertType.WARNING, "No previous page in history!");
            alert.show();
        } else {
            history.pop(); // Remove current page
            String url = history.peek(); // Get previous page
            loadWebsite(currentTab, url);
        }
    }

}

