module com.sbz.security {
    requires java.desktop;
    requires java.prefs;
    requires com.google.gson;
    requires com.google.common;
    requires miglayout;
    requires com.sbz.image;
    requires java.sql;
    opens com.sbz.security.data to com.google.gson;
}
