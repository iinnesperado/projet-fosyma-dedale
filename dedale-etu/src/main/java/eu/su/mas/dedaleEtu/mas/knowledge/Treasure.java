package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.time.LocalDateTime;

import eu.su.mas.dedale.env.Location;

public class Treasure implements Serializable {

    private static final long serialVersionUID = 7665186571536214170L;

    private Location position;
    private String type; // Exemple: "Gold" ou "Diamond"
    private Integer quantity;
    private boolean isOpen;
    private LocalDateTime recordTime;
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