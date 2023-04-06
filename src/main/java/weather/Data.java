package weather;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import weather.enums.Province;
import weather.enums.ReadingOf;
import weather.enums.Season;
import weather.enums.StationsOption;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Data {

    ArrayList<Station> stations;

    public Data() {
        stations = new ArrayList<>();
    }

    private static boolean isWithin(float distanceKM, float lat1, float lat2, float lon1, float lon2) {
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        distance = Math.pow(distance, 2);

        return (Math.sqrt(distance) <= distanceKM * 1000);
    }

    public float query(Query query) {
        ArrayList<Station> validStations = stations;

        if (query.province != null && query.province != Province.CANADA) {
            validStations = new ArrayList<>();
            for (Station s : stations) {
                if (s.getProvince() == query.province) {
                    validStations.add(s);
                }
            }
        }

        if (query.withinRangeKM != 0) {
            validStations = new ArrayList<>();
            for (Station s : stations) {
                if (isWithin(query.withinRangeKM, s.getLatitude(), query.latitude, s.getLongitude(), query.longitude)) {
                    validStations.add(s);
                }
            }
        }

        ArrayList<Station.Reading> validReadings = new ArrayList<>();
        for (Station s : validStations) {
            for (Station.Reading r : s.getReadings()) {
                if ((query.year == 0 || query.year == r.getYear())
                        && (query.month == 0 || query.month == r.getMonth())
                        && ((query.season == null || query.season == Season.ALL) || query.season == r.getSeason())) {
                    validReadings.add(r);
                }
            }
        }

        float total = 0, max = Float.MIN_VALUE, min = Float.MAX_VALUE;
        int count = 0;
        for (Station.Reading r : validReadings) {
            try {
                Field field = r.getClass().getDeclaredField(query.readingOf.toString());
                field.setAccessible(true);
                float value = (float) field.get(r);
                if (value != Float.MIN_VALUE) {
                    count++;
                    total += value;

                    if (value > max) {
                        max = value;
                    }

                    if (value < min) {
                        min = value;
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (count > 0) {
            return switch (query.stationsOption) {
                case AVERAGE -> total / count;
                case EXTREME_MAX -> max;
                case EXTREME_MIN -> min;
            };
        }
        return Float.MIN_VALUE;
    }

    public void load() {
        stations = new ArrayList<>();
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Station>>() {
            }.getType();
            stations = gson.fromJson(Files.readString(Paths.get("stations.json")), type);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void download() {
        try {
            ArrayList<Thread> threads = new ArrayList<>();
            ArrayList<DownloadMonthRunnable> months = new ArrayList<>();
            for (int i=1; i<13; i++){
                DownloadMonthRunnable m = new DownloadMonthRunnable(i);
                months.add(m);
                Thread t = new Thread(m);
                threads.add(t);
                t.start();
            }

            for (Thread t : threads){
                t.join();
            }

            HashMap<String, Station> stationsId = new HashMap<>();
            for (DownloadMonthRunnable dm : months){
                for (Station s : dm.getStations()){
                    if (stationsId.containsKey(s.getIdentifier())){
                        stationsId.get(s.getIdentifier()).getReadings().addAll(s.getReadings());
                    }else {
                        stationsId.put(s.getIdentifier(), s);
                    }
                }
            }

            stations = new ArrayList<>(stationsId.values());
            Files.writeString(Paths.get("stations.json"), new Gson().toJson(stations));
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    static class Query {
        final Province province;
        final Season season;
        int year, month;
        final ReadingOf readingOf;
        final float withinRangeKM, longitude, latitude;
        final StationsOption stationsOption;

        public Query(Query.Builder builder) {
            province = builder.province;
            season = builder.season;
            year = builder.year;
            month = builder.month;
            readingOf = builder.readingOf;
            withinRangeKM = builder.withinRangeKM;
            longitude = builder.longitude;
            latitude = builder.latitude;
            stationsOption = builder.stationsOption;
        }

        public void setYear(int year){
            this.year = year;
        }

        public static class Builder {
            private Province province;
            private Season season;
            private int year, month;
            private ReadingOf readingOf;
            private float withinRangeKM, longitude, latitude;
            private StationsOption stationsOption;

            public Builder() {
                province = null;
                season = null;
                year = 0;
                month = 0;
                readingOf = null;
                withinRangeKM = 0;
                longitude = 0;
                latitude = 0;
                stationsOption = null;
            }

            public Query.Builder province(Province province) {
                this.province = province;
                return this;
            }

            public Query.Builder season(Season season) {
                this.season = season;
                return this;
            }

            public Query.Builder year(int year) {
                this.year = year;
                return this;
            }

            public Query.Builder month(int month) {
                this.month = month;
                return this;
            }

            public Query.Builder readingOf(ReadingOf readingOf) {
                this.readingOf = readingOf;
                return this;
            }

            public Query.Builder withinRangeKM(float km, float latitude, float longitude) {
                this.latitude = latitude;
                this.longitude = longitude;
                this.withinRangeKM = km;
                return this;
            }

            public Query.Builder stationsOption(StationsOption stationsOption) {
                this.stationsOption = stationsOption;
                return this;
            }

            public Query build() {
                return new Query(this);
            }
        }
    }

    private static class DownloadMonthRunnable implements Runnable {

        private static final int START_YEAR = 1850, END_YEAR = 2023;
        private final int month;
        private final ArrayList<Station> stations;

        public DownloadMonthRunnable(int month) {
            this.month = month;
            stations = new ArrayList<>();
        }

        public String Get(int year) {
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse("https://climate.weather.gc.ca/prods_servs/cdn_climate_summary_report_e.html")).newBuilder();
            urlBuilder.addQueryParameter("intYear", String.valueOf(year));
            urlBuilder.addQueryParameter("intMonth", String.valueOf(month));
            urlBuilder.addQueryParameter("dataFormat", "xml");
            String url = urlBuilder.build().toString();
            System.out.println("GET " + url);
            Request request = new Request.Builder().url(url).build();
            OkHttpClient client = new OkHttpClient();

            try {
                Response response = client.newCall(request).execute();
                ResponseBody rb = response.body();

                if (rb != null){
                    String s = rb.string();
                    File file = new File("raw_stations.txt");
                    FileWriter fr = new FileWriter(file, true);
                    fr.write(s);
                    fr.close();

                    return s;
                }
                return Get(year);

            } catch (IOException e) {
                e.printStackTrace();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return Get(year);
            }
        }

        @Override
        public void run() {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                for (int i = START_YEAR; i < END_YEAR; i++) {
                    String xmlString = Get(i);
                    Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlString)));
                    NodeList stationsXml = doc.getElementsByTagName("station");
                    if (stationsXml.getLength() > 0) {
                        for (int k = 0; k < stationsXml.getLength(); k++) {

                            Station newStation = new Station(stationsXml.item(k), i, month);
                            Station existingStation = getStation(newStation.getIdentifier());
                            if (existingStation != null) {
                                existingStation.addReading(stationsXml.item(k), i, month);
                            } else {
                                stations.add(newStation);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Station getStation(String id) {
            for (Station station : stations) {
                if (station.getIdentifier().equals(id)) {
                    return station;
                }
            }
            return null;
        }

        public ArrayList<Station> getStations() {
            return stations;
        }
    }
}
