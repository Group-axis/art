package message.partner.routing.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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

	@ManyToOne
	@JoinColumn(name = "connectionMethodName")
	private ConnectionMethod connectionMethod;
	private Boolean authenticationRequired;
	private String allowedDirection;
	private String sessionInitiation;

	@ManyToOne
	@JoinColumn(name = "emissionDetailsId")
	private EmissionDetails emissionDetails;
	private String  profileName;

	public MessagePartner() {

	}
}
