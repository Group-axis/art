package message.partner.routing.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Entity
@AllArgsConstructor
public class MessagePartner {

	@Id
	private String identifier;
	private String description;

	@JoinColumn(name = "connectionMethodName")
	private ConnectionMethod connectionMethod;
	private Boolean authenticationRequired;
	private String allowedDirection;
	private String sessionInitiation;

	@JoinColumn(name = "emissionDetailsId")
	private EmissionDetails emissionDetails;
	@JoinColumn(name = "profileName")
	private Profile profile;

	public MessagePartner() {

	}
}
