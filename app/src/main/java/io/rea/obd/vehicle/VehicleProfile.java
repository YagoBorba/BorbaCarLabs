package io.rea.obd.vehicle;

public final class VehicleProfile {
    public final String engine;
    public final String fuel;
    public final String id;
    public final String make;
    public final String market;
    public final String model;
    public final String name;
    public final String notes;
    public final String transmission;
    public final String trim;
    public final Integer year;

    public VehicleProfile(String id, String name, String display, String model, Integer num, String market, String display2, String engine, String fuelType, String transmission, String notes) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Identificador e nome são obrigatórios");
        }
        if (num != null && (num.intValue() < 1886 || num.intValue() > 2100)) {
            throw new IllegalArgumentException("Ano inválido");
        }
        this.id = id;
        this.name = name.trim();
        this.make = clean(display);
        this.model = clean(model);
        this.year = num;
        this.market = clean(market);
        this.trim = clean(display2);
        this.engine = clean(engine);
        this.fuel = clean(fuelType);
        this.transmission = clean(transmission);
        this.notes = clean(notes);
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }

    public String description() {
        String text;
        StringBuilder builder = new StringBuilder();
        builder.append(this.make);
        builder.append(" ");
        builder.append(this.model);
        builder.append(" ");
        builder.append(this.trim);
        if (this.year == null) {
            text = "";
        } else {
            text = " " + this.year;
        }
        builder.append(text);
        String text2 = builder.toString().trim().replaceAll(" +", " ");
        return text2.isEmpty() ? this.notes : text2;
    }
}