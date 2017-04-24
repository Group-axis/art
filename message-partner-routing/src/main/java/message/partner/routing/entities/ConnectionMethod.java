package message.partner.routing.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ConnectionMethod {

    @Id
    private String name;
    private String dataFormat;
    private String queueManagerName;
    private String queueName;
    private String errorQueueName;
    private Boolean keepSessionOpen;
    private Boolean transferSAAInfo;
    private Boolean useMQDescriptor;
    private Boolean includeTrn;
    private Boolean removeSBlock;
    private Boolean uniqueMesgID;
    private Boolean useBinaryPrefixFormat;

    public ConnectionMethod() {

    }
}
