package com.truvira.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.webp.WebpDirectory;
import com.truvira.model.ImageMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Reads EXIF metadata from an image file that is ALREADY saved on disk.
 *
 * EXIF = "Exchangeable Image File Format": extra data a camera/phone writes inside the photo
 * (camera model, date taken, GPS, editing software...). The pixels are NOT touched; we only read.
 *
 * FORENSICS RULE: missing EXIF is NOT proof of manipulation. WhatsApp, Instagram, screenshots and
 * AI image generators usually have none. EXIF can also be edited by anyone, so it is a CLUE, never proof.
 *
 * It is a separate @Service (not inside EvidenceService) = "single responsibility":
 * one class, one job. It also makes it easy to unit-test on its own later.
 */
@Service
public class MetadataService {

    // LOGGER: prints messages to the console (like System.out.println, but with time, level and class name).
    // SLF4J is the logging "front" that Spring Boot already includes. static final = one shared logger per class.
    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    public ImageMetadata extract(Path imageFile) {
        try {
            // The library detects the format (JPEG/PNG/WEBP) and parses it into "directories".
            // A Directory = one group of tags, e.g. "Exif IFD0", "GPS", "JPEG".
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile.toFile());

            // getFirstDirectoryOfType returns the group, or null if the image doesn't have it.
            // ".class" = a reference to the class itself, used here to say WHICH type we want.
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);   // main EXIF: make, model, software
            ExifSubIFDDirectory sub = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class); // camera details: date taken
            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);              // location

            boolean exifFound = ifd0 != null || sub != null || gps != null;

            // GPS: the library converts degrees/minutes/seconds into one decimal number for us.
            GeoLocation location = (gps == null) ? null : gps.getGeoLocation();

            return new ImageMetadata(
                    exifFound,
                    text(ifd0, ExifIFD0Directory.TAG_MAKE),
                    text(ifd0, ExifIFD0Directory.TAG_MODEL),
                    text(ifd0, ExifIFD0Directory.TAG_SOFTWARE),
                    text(sub, ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL),
                    dimension(metadata, true),
                    dimension(metadata, false),
                    location == null ? null : location.getLatitude(),
                    location == null ? null : location.getLongitude()
            );

        } catch (ImageProcessingException | IOException e) {
            // MULTI-CATCH (the "|"): one block handles both exception types.
            // We do NOT re-throw: a broken/unusual file must still be stored as evidence.
            log.warn("Could not read metadata from {}: {}", imageFile.getFileName(), e.getMessage());
            return ImageMetadata.empty();
        }
    }

    /** Safely read a text tag; returns null if the directory or the tag is missing. */
    private String text(Directory dir, int tag) {
        if (dir == null) return null;
        String value = dir.getString(tag);
        // Cameras often pad values with spaces or invisible "\0" characters; strip() removes them.
        return (value == null || value.strip().isEmpty()) ? null : value.strip();
    }

    /**
     * Width/height are stored in a DIFFERENT place for each file format, so we try each one.
     * The boolean parameter chooses width (true) or height (false).
     */
    private Integer dimension(Metadata metadata, boolean width) {
        JpegDirectory jpeg = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        if (jpeg != null) return jpeg.getInteger(width ? JpegDirectory.TAG_IMAGE_WIDTH : JpegDirectory.TAG_IMAGE_HEIGHT);

        PngDirectory png = metadata.getFirstDirectoryOfType(PngDirectory.class);
        if (png != null) return png.getInteger(width ? PngDirectory.TAG_IMAGE_WIDTH : PngDirectory.TAG_IMAGE_HEIGHT);

        WebpDirectory webp = metadata.getFirstDirectoryOfType(WebpDirectory.class);
        if (webp != null) return webp.getInteger(width ? WebpDirectory.TAG_IMAGE_WIDTH : WebpDirectory.TAG_IMAGE_HEIGHT);

        return null; // format didn't tell us
    }
}
