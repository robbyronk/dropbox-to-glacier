package in.ayecapta.photobackup.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import in.ayecapta.photobackup.PhotoBackup;

import javax.enterprise.inject.Produces;

public class AwsCredentials {
    @Produces
    @PhotoBackup
    public AWSCredentials produceCredentials() {
        String accessKey = "";
        String secretKey = "";
        return new BasicAWSCredentials(accessKey, secretKey);
    }
}
