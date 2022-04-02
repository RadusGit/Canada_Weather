package weather.enums;

public enum Province {
    NL("Newfoundland and Labrador"),
    PE("Prince Edward Island"),
    NS("Nova Scotia"),
    NB("New Brunswick"),
    QC("Quebec"),
    ON("Ontario"),
    MB("Manitoba"),
    SK("Saskatchewan"),
    AB("Alberta"),
    BC("British Columbia"),
    YT("Yukon"),
    NT("Northwest Territories"),
    NU("Nunavut"),
    CANADA("Canada");

    private final String text;

    Province(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
