# Future Ideas
- Automated retrieval
- Support any source for photos
- Support RAW photos
- Support videos
- Optimize storage/retrieval for cost

# Future Architecture
Pipeline of bringing images into the system.
1. Source to S3 (original), to bring the image into the system. Create metadata for image, validate image.
2. Optionally delete image from source.
3. Gzip a copy of the image from S3 (original) and upload it to S3 (archive).
4. Verify the archive image.
5. Create thumbnail from S3 (original) and upload it to S3 (thumbnails).
6. Delete image from S3 (original).

# Retrieval of images
Retrieval requests go into a queue.

If more than 4 hours have passed since the last retrieval request was processed, initiate restore on the first in the queue.

Process listens on SNS and updates database with link to original. Email is sent, file is uploaded to their dropbox.
