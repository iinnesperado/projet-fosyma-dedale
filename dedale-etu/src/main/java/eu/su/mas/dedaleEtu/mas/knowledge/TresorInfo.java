package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

/**
 * Représente un trésor (or ou diamant) avec son type et sa position (id de
 * noeud).
 */
public class TresorInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type; // Exemple: "Gold" ou "Diamond"
    private String positionId; // Exemple: "G5"

    public TresorInfo(String type, String positionId) {
        this.type = type;
        this.positionId = positionId;
    }

    public String getType() {
        return type;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    @Override
    public String toString() {
        return "Tresor: " + type + " à " + positionId;
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
