package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;

/**
 * Représente un trésor (or ou diamant) avec son type et sa position (id de
 * noeud).
 */
public class TresorInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Observation type; // Exemple: "Gold" ou "Diamond"
    private String positionId; // Exemple: "G5"
    private Integer quantity; // Quantité d'or ou de diamants

    public TresorInfo(Observation type, String positionId, Integer quantity) {
        this.type = type;
        this.positionId = positionId;
        this.quantity = quantity;

    }

    public Observation getType() {
        return type;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setType(Observation type) {
        this.type = type;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return quantity + " " + type + " en position " + positionId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof TresorInfo))
            return false;
        TresorInfo other = (TresorInfo) obj;
        return this.type.equals(other.type) && this.positionId.equals(other.positionId);
    }

    @Override
    public int hashCode() {
        return type.hashCode() + positionId.hashCode();
    }

}
