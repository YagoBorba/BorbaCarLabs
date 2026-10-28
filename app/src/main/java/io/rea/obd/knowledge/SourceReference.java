package io.rea.obd.knowledge;

public final class SourceReference {
    public final String accessedOn;
    public final Distribution distribution;
    public final String id;
    public final String location;
    public final String publisher;
    public final String review;
    public final String scope;
    public final String section;
    public final String title;
    public final String version;

    public enum Distribution {
        LINK_METADATA_ONLY
    }

    public SourceReference(String id, String title, String publisher, String version, String location, String section, String accessedOn, String scope, String review) {
        String[] parts = {id, title, publisher, version, location, section, accessedOn, scope, review};
        for (int i = 0; i < 9; i++) {
            String text = parts[i];
            if (text == null || text.isEmpty()) {
                throw new IllegalArgumentException("Referência incompleta");
            }
        }
        if (!location.startsWith("https://")) {
            throw new IllegalArgumentException("Localização de fonte inválida");
        }
        this.id = id;
        this.title = title;
        this.publisher = publisher;
        this.version = version;
        this.location = location;
        this.section = section;
        this.accessedOn = accessedOn;
        this.scope = scope;
        this.review = review;
        this.distribution = Distribution.LINK_METADATA_ONLY;
    }

    public String display() {
        return this.title + " · " + this.publisher + " · " + this.version + "\n" + this.location + "\nSeção: " + this.section + " · consulta " + this.accessedOn + "\nEscopo: " + this.scope + "\n" + this.review + "\nSomente metadados e link; documento/texto técnico não redistribuído.";
    }
}