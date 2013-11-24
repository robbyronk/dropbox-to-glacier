dropbox-to-glacier
==================

Move images from dropbox to glacier with a thumbnail in s3

# Setup
Create `/src/main/resources/dropbox.properties`:
```ini
app_key=<your dropbox app key>
app_secret=<your dropbox app secret>
accessToken=<your dropbox accessToken>
```

Create `/src/main/resources/s3.properties`:
```ini
accessKey=<your amazon access key>
secretKey=<your amazon secret key>
archiveBucket=<name of the s3 bucket to store compressed files>
thumbnailBucket=<name of the s3 bucket to store thumnails of images>
```

Run `mvn compile assembly:single` to build a runnable jar.

# Run
`java -jar target/*-with-dependencies.jar`
