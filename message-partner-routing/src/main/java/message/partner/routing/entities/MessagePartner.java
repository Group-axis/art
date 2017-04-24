package message.partner.routing.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class MessagePartner {

    @Id
    private String identifier;
    private String description;
    private ConnectionMethod connectionMethod;
    private Boolean authenticationRequired;
    private String allowedDirection;
    private String sessionInitiation;
    private EmissionDetails emissionDetails;
    private Profile profile;

    public MessagePartner() {

    }
}
