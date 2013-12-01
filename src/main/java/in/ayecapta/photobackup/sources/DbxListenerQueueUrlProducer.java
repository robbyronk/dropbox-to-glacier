package in.ayecapta.photobackup.sources;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import in.ayecapta.photobackup.PhotoBackup;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.regex.Pattern;

public class DbxListenerQueueUrlProducer {
    public static final String DBX_LISTENER = "DbxListener";
    @Inject
    @PhotoBackup
    AmazonSQS sqsClient;

    @Produces
    @DbxListenerQueueUrl
    public String getUrl() {
        return Iterables.find(sqsClient.listQueues().getQueueUrls(),
                Predicates.contains(Pattern.compile(DBX_LISTENER)),
                sqsClient.createQueue(new CreateQueueRequest(DBX_LISTENER)).getQueueUrl());
    }
}
