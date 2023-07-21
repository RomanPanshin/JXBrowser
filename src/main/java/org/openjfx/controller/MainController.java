package org.openjfx.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
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
    public void initialize() {
        goButton.setOnAction(this::loadUrl);
        newTabButton.setOnAction(this::newTab);

        // Handle when a tab is selected.
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            WebView webView = (WebView) newTab.getContent();
            urlField.setText(webView.getEngine().getLocation());
        });

        // Load default URL in the first tab.
        loadWebsite(tabPane.getTabs().get(0), "https://www.google.com");
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
    }

    private void loadWebsite(Tab tab, String url) {
        WebView webView = (WebView) tab.getContent();
        WebEngine webEngine = webView.getEngine();

        if (url.endsWith(".pdf")) {
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
        } else {
            // Load URL.
            webEngine.load(url);
        }

        webEngine.locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                urlField.setText(newValue);
                tab.setText(getWebsiteTitle(newValue));
            }
        });

        tab.setContent(webView);
    }

    private String getWebsiteTitle(String url) {
        if (url == null || url.isEmpty()) {
            return "New Tab";
        }


        try {
            String domain = new java.net.URL(url).getHost();
            if (url.contains("viewer.html"))
                return "Pdf File";
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (Exception e) {
            e.printStackTrace();
            return "New Tab";
        }
    }
}