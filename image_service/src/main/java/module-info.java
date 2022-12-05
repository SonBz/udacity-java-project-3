module com.sbz.image {
    exports com.sbz.image to com.sbz.security;
    requires org.slf4j;
    requires java.desktop;
    requires software.amazon.awssdk.services.rekognition;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
}
