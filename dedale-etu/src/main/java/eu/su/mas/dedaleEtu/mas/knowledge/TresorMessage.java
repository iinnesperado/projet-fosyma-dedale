package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class TresorMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<TresorInfo> tresors;
    private LocalDateTime dateDerniereMaj;

    public TresorMessage(List<TresorInfo> tresors, LocalDateTime dateDerniereMaj) {
        this.tresors = tresors;
        this.dateDerniereMaj = dateDerniereMaj;
    }

    public List<TresorInfo> getTresors() {
        return tresors;
    }

    public LocalDateTime getDateDerniereMaj() {
        return dateDerniereMaj;
    }

    @Override
    public String toString() {
        return "Maj: " + dateDerniereMaj + " | Trésors: " + tresors;
    }
}
