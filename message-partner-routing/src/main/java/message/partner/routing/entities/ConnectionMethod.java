package message.partner.routing.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Entity
@AllArgsConstructor
public class ConnectionMethod {

    @Id
    private String connectionMethodName;
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
