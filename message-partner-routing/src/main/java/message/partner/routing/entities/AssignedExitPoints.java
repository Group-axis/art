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
public class AssignedExitPoints {
	@Id
	private String exitPointName;

	public AssignedExitPoints() {

	}
}
