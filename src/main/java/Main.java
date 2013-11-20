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
import java.util.zip.GZIPOutputStream;

import static org.apache.commons.io.CopyUtils.copy;

public class Main {
    {
        final String APP_KEY = "123";
        final String APP_SECRET = "456";
    }

    public static final String accessToken = "aaa";
    private static final String bucketName = "buckets";
    private static AmazonS3Client s3;

    public static void main(String[] args) throws IOException, DbxException {

        s3 = new AmazonS3Client(new PropertiesCredentials(Main.class.getResourceAsStream("s3.properties")));
//
//        s3.putObject(new PutObjectRequest("bucketname", key, inputStream, metadata));

        DbxRequestConfig config = new DbxRequestConfig("JavaTutorial/1.0", Locale.getDefault().toString());
//        DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);
//
//        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
//
//        // Have the user sign in and authorize your app.
//        String authorizeUrl = webAuth.start();
//        System.out.println("1. Go to: " + authorizeUrl);
//        System.out.println("2. Click \"Allow\" (you might have to log in first)");
//        System.out.println("3. Copy the authorization code.");
//        String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
//
//        // This will fail if the user enters an invalid authorization code.
//        DbxAuthFinish authFinish = webAuth.finish(code);

        DbxClient client = new DbxClient(config, accessToken);

        System.out.println("Linked account: " + client.getAccountInfo().displayName);
        System.out.println("Access Token: " + client.getAccessToken());

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