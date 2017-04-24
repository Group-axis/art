package message.partner.routing.entities;

import javax.persistence.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Entity
@AllArgsConstructor
public class Profile {
    private String name;

    public Profile() {

    }

}
