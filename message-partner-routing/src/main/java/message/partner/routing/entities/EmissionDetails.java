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
public class EmissionDetails {

	@Id
	private Long emissionDetailsId;
    private Boolean alwaysTransferMacPac;
    private Boolean transferPkiSignature;
    private Boolean incrementSequenceAccrossSession;
    
    @ManyToOne
    @JoinColumn(name="accessEmissionDetailsId")
    private AccessEmissionDetails accessEmissionDetails;
    private String language;

    public EmissionDetails() {

    }
}
