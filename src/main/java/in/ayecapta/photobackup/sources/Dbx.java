package in.ayecapta.photobackup.sources;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import in.ayecapta.photobackup.PhotoBackup;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

// Listens to an amazon queue for dropbox tokens
// Lists files in the users dropbox folder "MoveToGlacier"
// For each of the files, add it to a queue to download from dropbox and store in the originals bucket
public class Dbx {

    @Inject
    @PhotoBackup
    private AmazonSQSAsync sqsClient;

    @Inject
    @DbxListenerQueueUrl
    String queueUrl;

    public void listen() {
        Future<ReceiveMessageResult> message = sqsClient.receiveMessageAsync(new ReceiveMessageRequest(queueUrl));
        ReceiveMessageResult receiveMessageResult = null;
        try {
            receiveMessageResult = message.get();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ExecutionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        List<Message> messages = receiveMessageResult.getMessages();
        for (Message message1 : messages) {
            message1.getBody();
        }
    }
}
