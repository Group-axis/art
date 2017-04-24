package message.partner.routing.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Entity
@AllArgsConstructor
public class AccessEmissionDetails {

	@Id
	private Long id;

	@JoinColumn(name = "exitPointName")
	private AssignedExitPoints assignedExitPoints;
	private Boolean routingCodeTransmitted;
	private String messageEmissionFormat;
	private String notificationIncludesOriginalMessage;
	private String originalMessageFormat;
	private Boolean transferUUMID;

	public AccessEmissionDetails() {

	}
}
