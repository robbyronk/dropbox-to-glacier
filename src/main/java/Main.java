import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.dropbox.core.*;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import static org.apache.commons.io.CopyUtils.copy;

public class Main {
    private static final String bucketName = "buckets";
    private static AmazonS3Client s3;

    private static DbxClient connectToDropbox() {
        Properties properties = new Properties();
        try {
            InputStream stream = Main.class.getResourceAsStream("dropbox.properties");
            properties.load(stream);
            stream.close();
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

        s3 = new AmazonS3Client(new PropertiesCredentials(Main.class.getResourceAsStream("s3.properties")));
//
//        s3.putObject(new PutObjectRequest("bucketname", key, inputStream, metadata));


        DbxClient client = connectToDropbox();

        System.out.println("Linked account: " + client.getAccountInfo().displayName);

        client.createFolder("/Test");
        DbxEntry.WithChildren listing = client.getMetadataWithChildren("/Test");
        System.out.println("Files in the root path:");
        for (DbxEntry child : listing.children) {
            DbxClient.Downloader downloader = client.startGetFile(child.path, null);

            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            OutputStream gzipOutputStream = new GZIPOutputStream(compressed);
            try {
                InputStream teeInputStream = new TeeInputStream(downloader.body, gzipOutputStream);
                thumbnailer(teeInputStream);
                gzipOutputStream.close();
                upload(bucketName, "kitty.gz", compressed.toByteArray());
                compressed.close();
            } finally {
                downloader.close();
            }
        }
    }

    public static void thumbnailer(InputStream inputStream) throws IOException {
        ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
        ImageReader imageReader = Iterators.getOnlyElement(ImageIO.getImageReaders(imageInputStream));
        imageReader.setInput(imageInputStream);

        BufferedImage webSize = Scalr.resize(imageReader.read(0), 1024);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String formatName = imageReader.getFormatName().toLowerCase();
        ImageIO.write(webSize, formatName, baos);

        upload(bucketName, "kitty." + formatName, baos.toByteArray());
    }

    public static void upload(String bucket, String name, byte[] buf) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(buf.length);
        s3.putObject(bucket, name, new ByteArrayInputStream(buf), metadata);
    }
}