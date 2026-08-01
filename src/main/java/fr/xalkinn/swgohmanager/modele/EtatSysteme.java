package fr.xalkinn.swgohmanager.modele;

import java.time.LocalDateTime;

public class EtatSysteme {


    private boolean bddOk;
    private boolean comlinkOk;
    private LocalDateTime derniereSynchro;
    private String derniereSynchroFormatee;

    public boolean isBddOk() {
        return bddOk;
    }

    public void setBddOk(boolean bddOk) {
        this.bddOk = bddOk;
    }

    public boolean isComlinkOk() {
        return comlinkOk;
    }

    public void setComlinkOk(boolean comlinkOk) {
        this.comlinkOk = comlinkOk;
    }

    public LocalDateTime getDerniereSynchro() {
        return derniereSynchro;
    }

    public void setDerniereSynchro(LocalDateTime derniereSynchro) {
        this.derniereSynchro = derniereSynchro;
    }
    
    @Override
    public String toString() {

        return "EtatSysteme {" +
                "bddOk=" + bddOk +
                ", comlinkOk=" + comlinkOk +
                ", derniereSynchro=" + derniereSynchro +
                '}';

    }
    public String getDerniereSynchroFormatee() {
        return derniereSynchroFormatee;
    }


    public void setDerniereSynchroFormatee(String derniereSynchroFormatee) {
        this.derniereSynchroFormatee = derniereSynchroFormatee;
    }
}