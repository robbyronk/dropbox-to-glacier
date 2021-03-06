package in.ayecapta.dbx2s3;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.dropbox.core.*;
import com.google.common.collect.Iterators;
import org.apache.commons.io.input.TeeInputStream;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

public class OldMain {
    private static AmazonS3Client s3;
    private static String archiveBucket;
    private static String thumbnailBucket;

    private static DbxClient connectToDropbox() {
        Properties properties = new Properties();
        try {
            InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("dropbox.properties");
            properties.load(resourceAsStream);
            resourceAsStream.close();
        } catch (IOException e) {
            System.err.println("Unable to read dropbox.properties, can't continue. Sorry!");
            throw new RuntimeException(e);
        }

        assert properties.stringPropertyNames().contains("app_key");
        assert properties.stringPropertyNames().contains("app_secret");

        DbxRequestConfig config = new DbxRequestConfig("dropbox-to-s3/0.1", Locale.getDefault().toString());
        if (properties.stringPropertyNames().contains("accessToken")) {
            return new DbxClient(config, properties.getProperty("accessToken"));
        }

        DbxAppInfo appInfo = new DbxAppInfo(properties.getProperty("app_key"), properties.getProperty("app_secret"));

        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);

        // Have the user sign in and authorize your app.
        String authorizeUrl = webAuth.start();
        System.out.println("1. Go to: " + authorizeUrl);
        System.out.println("2. Click \"Allow\" (you might have to log in first)");
        System.out.println("3. Copy the authorization code.");
        try {
            String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
            String accessToken = webAuth.finish(code).accessToken;
            System.out.println("Add `accessToken=" + accessToken + "` to your dropbox.properties");
            return new DbxClient(config, accessToken);
        } catch (IOException e) {
            System.err.println("Unable to read the code from STDIN, just add the code to dropbox.properties.");
            throw new RuntimeException(e);
        } catch (DbxException e) {
            System.err.println("Invalid authorization code. Try again...");
            System.exit(1);
            return null;
        }
    }

    public static void main(String[] args) throws IOException, DbxException {

        Properties properties = new Properties();
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("s3.properties");
        properties.load(resourceAsStream);
        archiveBucket = properties.getProperty("archiveBucket");
        thumbnailBucket = properties.getProperty("thumbnailBucket");
        s3 = new AmazonS3Client(new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey")));

        DbxClient client = connectToDropbox();

        System.out.println("Linked account: " + client.getAccountInfo().displayName);

        String path = "/Test";
        client.createFolder(path);
        System.out.println("Files in the root path:");
        listChildren(client.getMetadataWithChildren(path), client);
    }

    private static void listChildren(DbxEntry.WithChildren listing, DbxClient client) throws DbxException {
        for (DbxEntry child : listing.children) {
            if (child.isFile()) {
                System.out.println(child.path);

                DbxClient.Downloader downloader = client.startGetFile(child.path, null);
                InputStream body = downloader.body;
                try {
                    copyToS3(body, child.path);
                    client.delete(child.path);
                } catch (Exception e) {
                    System.err.print("Problem copying " + child.path + ". ");
                    System.err.println("DO NOT DELETE.");
                } finally {
                    downloader.close();
                }

            }
            if (child.isFolder()) {
                listChildren(client.getMetadataWithChildren(child.path), client);
            }
        }
    }

    private static void copyToS3(InputStream body, String path) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        OutputStream gzipOutputStream = new GZIPOutputStream(compressed);
        InputStream teeInputStream = new TeeInputStream(body, gzipOutputStream);
        try {
            thumbnailer(teeInputStream, path);
            gzipOutputStream.close();
            upload(archiveBucket, path + ".gz", compressed.toByteArray());
        } finally {
            teeInputStream.close();
            compressed.close();
        }
    }

    private static void thumbnailer(InputStream inputStream, String path) throws IOException {
        ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
        ImageReader imageReader = Iterators.getOnlyElement(ImageIO.getImageReaders(imageInputStream));
        imageReader.setInput(imageInputStream);

        BufferedImage webSize = Scalr.resize(imageReader.read(0), 1024);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String formatName = imageReader.getFormatName().toLowerCase();
        ImageIO.write(webSize, formatName, baos);

        upload(thumbnailBucket, path, baos.toByteArray());
        inputStream.close();
        imageInputStream.close();
        imageReader.dispose();
        baos.close();
    }

    private static void upload(String bucket, String name, byte[] buf) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(buf.length);
        s3.putObject(bucket, name, new ByteArrayInputStream(buf), metadata);
    }
}