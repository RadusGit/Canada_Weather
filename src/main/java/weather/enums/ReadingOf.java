package weather.enums;

public enum ReadingOf {
    MIN_TEMP("minTemp"),
    MEAN_TEMP("meanTemp"),
    MAX_TEMP("maxTemp"),
    SNOW("snow"),
    PRECIPITATION("precipitation");

    private final String text;

    ReadingOf(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
