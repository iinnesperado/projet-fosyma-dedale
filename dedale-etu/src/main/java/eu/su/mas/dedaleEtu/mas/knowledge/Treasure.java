package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;

/**
 * @param requirements (lockPicking, strength)
 */
public class Treasure implements Serializable {

    private static final long serialVersionUID = 7665186571536214170L;

    private Location position;
    private String type; // Exemple: "Gold" ou "Diamond"
    private Integer quantity;
    private boolean isOpen;
    private LocalDateTime recordTime;
    private Couple<Integer, Integer> requirements;
    // private boolean inDanger; // Optional - allows to record if a golem is
    // nearby, will make an indicator for priority

    public Treasure(Location pos, String type) {
        this.position = pos;
        this.type = type;
    }

    public Treasure(Location pos, String type, Integer quantity, LocalDateTime recordTime) {
        this.position = pos;
        this.type = type;
        this.quantity = quantity;
        this.recordTime = recordTime;
    }

    public Treasure(Location pos, LocalDateTime recorTime) {
        this.position = pos;
        this.recordTime = recorTime;
    }

    public String getType() {
        return type;
    }

    public Location getPosition() {
        return position;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPosition(Location pos) {
        this.position = pos;
    }

    public String getPositionID() {
        return this.position.getLocationId();
    }

    public void setRecordTime(LocalDateTime time) {
        this.recordTime = time;
    }

    public LocalDateTime getRecordTime() {
        return this.recordTime;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuantity() {
        return this.quantity;

    }

    public void setIsOpen(boolean status) {
        this.isOpen = status;
    }

    public boolean getIsOpen() {
        return this.isOpen;
    }

    public Couple<Integer, Integer> setRequiredSkills(List<Couple<Location, List<Couple<Observation, String>>>> observations) {
        for (Couple<Location, List<Couple<Observation, String>>> node : observations) {
            if (node.getLeft().equals(position)) {
                Integer lockpickingNeeded = null;
                Integer strengthNeeded = null;
                
                for (Couple<Observation, String> obs : node.getRight()) {
                    switch (obs.getLeft().getName()) {
                        case "LockPicking":
                            lockpickingNeeded = Integer.parseInt(obs.getRight());
                            break;
                        case "Strength":
                            strengthNeeded = Integer.parseInt(obs.getRight());
                            break;
                    }
                }
                return new Couple<>(lockpickingNeeded, strengthNeeded);
            }
        }
        return new Couple<>(0 ,0 ); // Valeurs par défaut si non trouvé
    }

    public Couple<Integer, Integer> getRequiredSkills(){
        return requirements;
    }

    @Override
    public String toString() {
        return "Tresor: " + type + " (" + quantity + ") en position " + position.getLocationId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Treasure))
            return false;
        Treasure other = (Treasure) obj;
        return this.type.equals(other.type) && this.position.equals(other.position);
    }

    @Override
    public int hashCode() {
        return type.hashCode() + position.hashCode();
    }
}