package weather;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import weather.enums.Province;
import weather.enums.Season;

import java.util.ArrayList;

public class Station {
    private final String name;
    private final String identifier;
    private final Province province;
    private final ArrayList<Reading> readings;
    private final float latitude, longitude;

    public Station(Node stationNode, int year, int month, Province province) {
        this.province = province;
        name = getElementValue(stationNode, "name");
        identifier = getElementValue(stationNode, "identifier");

        String la = getElementValue(stationNode, "latitude");
        String lo = getElementValue(stationNode, "longitude");
        latitude = la == null ? Float.MIN_VALUE : Float.parseFloat(la);
        longitude = lo == null ? Float.MIN_VALUE : Float.parseFloat(lo);

        readings = new ArrayList<>();
        readings.add(new Reading(stationNode, year, month));
    }

    private static String getElementValue(Node stationNode, String tagName) {
        NodeList childNodes = stationNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeName().equals(tagName)) {
                return childNodes.item(i).getTextContent();
            }
        }
        return null;
    }

    private static String getAttributeValue(Node stationNode, String tagName, String attributeName) {
        NodeList childNodes = stationNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeName().equals(tagName)) {
                NamedNodeMap attr = childNodes.item(i).getAttributes();
                Node attrNode = attr.getNamedItem(attributeName);
                if (attrNode != null) {
                    return attrNode.getTextContent();
                }
            }
        }
        return null;
    }

    private static float getAttributeValuefloat(Node stationNode, String tagName, String attributeName) {
        String value = getAttributeValue(stationNode, tagName, attributeName);
        if (value != null) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return Float.MIN_VALUE;
    }

    public void addReading(Node stationNode, int year, int month) {
        readings.add(new Reading(stationNode, year, month));
    }

    public String getName() {
        return name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Province getProvince() {
        return province;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public ArrayList<Reading> getReadings() {
        return readings;
    }

    static class Reading {
        private final int year;
        private final int month;
        private final float minTemp;
        private final float meanTemp;
        private final float maxTemp;
        private final float snow;
        private final float precipitation;

        public Reading(Node stationNode, int year, int month) {
            this.year = year;
            this.month = month;
            minTemp = getAttributeValuefloat(stationNode, "min_temperature", "value");
            meanTemp = getAttributeValuefloat(stationNode, "mean_temperature", "value");
            maxTemp = getAttributeValuefloat(stationNode, "max_temperature", "value");
            snow = getAttributeValuefloat(stationNode, "snow", "total");
            precipitation = getAttributeValuefloat(stationNode, "precipitation", "total");
        }

        public int getMonth() {
            return month;
        }

        public int getYear() {
            return year;
        }

        // ;)
        public Season getSeason() {
            if (month == 3 || month == 4 || month == 5) {
                return Season.SPRING;
            }
            if (month == 6 || month == 7 || month == 8) {
                return Season.SUMMER;
            }
            if (month == 9 || month == 10 || month == 11) {
                return Season.AUTUMN;
            }
            if (month == 12 || month == 1 || month == 2) {
                return Season.WINTER;
            }
            return null;
        }
    }
}
