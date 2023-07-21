module org.openjfx {
    requires org.apache.commons.io;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;
    requires javafx.swing;

//    requires commons.io; // Add this line to require the commons.io module

    opens org.openjfx to javafx.fxml;
    opens org.openjfx.controller to javafx.fxml;

    exports org.openjfx;
    exports org.openjfx.controller;
}
