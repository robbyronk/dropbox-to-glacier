package in.ayecapta.photobackup.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import in.ayecapta.photobackup.PhotoBackup;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static com.amazonaws.services.sqs.buffered.QueueBufferConfig.LONGPOLL_WAIT_TIMEOUT_SECONDS_DEFAULT;

public class SqsClient {
    @Inject
    @PhotoBackup
    private AWSCredentials awsCredentials;

    @Produces
    @PhotoBackup
    private AmazonSQSAsync produceAsyncClient() {
        AmazonSQSAsync sqs = new AmazonSQSAsyncClient(awsCredentials);
        QueueBufferConfig config = new QueueBufferConfig()
                .withLongPoll(true)
                .withLongPollWaitTimeoutSeconds(LONGPOLL_WAIT_TIMEOUT_SECONDS_DEFAULT);
        return new AmazonSQSBufferedAsyncClient(sqs, config);
    }

    @Produces
    @PhotoBackup
    private AmazonSQS produceClient() {
        return new AmazonSQSClient(awsCredentials);
    }
}
