package sds.home.homebutler.dto;

public class DatabaseConnexionConfig {

    private String hote;
    private Integer port;
    private String nomBase;
    private String utilisateur;
    private String motDePasse;

    // true si une configuration a déjà été enregistrée (fichier config/datasource.properties présent).
    private boolean configuree;

    public DatabaseConnexionConfig() {
    }

    public String getHote() {
        return hote;
    }

    public void setHote(String hote) {
        this.hote = hote;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getNomBase() {
        return nomBase;
    }

    public void setNomBase(String nomBase) {
        this.nomBase = nomBase;
    }

    public String getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(String utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public boolean isConfiguree() {
        return configuree;
    }

    public void setConfiguree(boolean configuree) {
        this.configuree = configuree;
    }

}
