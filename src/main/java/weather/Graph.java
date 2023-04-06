package weather;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.time.Year;
import org.jfree.data.xy.XYDataset;
import weather.enums.Province;
import weather.enums.ReadingOf;
import weather.enums.Season;
import weather.enums.StationsOption;

import java.awt.*;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Queue;

public class Graph {
    final Data data;
    private final static int ROLLING_NUMBER = 7;

    Graph() {
        data = new Data();
    }

    public static void main(String[] args) {
        Graph g = new Graph();

        if (!new File("stations.json").exists()){
            g.getData().download();
        }

        g.getData().load();
        g.makeGraphs();

//        Data.Query query = new Data.Query.Builder()
//                .readingOf(ReadingOf.MAX_TEMP)
//                .stationsOption(StationsOption.AVERAGE)
//                .province(Province.CANADA)
//                .season(Season.ALL)
//                .build();
//
//        g.createChart(new File("test1.png"), "1900", 1900, 2017, query);
    }

    public Data getData() {
        return data;
    }

    private void makeGraphs() {
        int startYear = 1900, endYear = 2022;
        for (Province province : Province.values()) {
            (new Thread(() -> {
                for (ReadingOf readingOf : ReadingOf.values()) {
                    for (Season season : Season.values()) {
                        for (StationsOption stationsOption : StationsOption.values()) {
                            String title = stationsOption.name() + " " + readingOf.name() + " in " + province.toString().toUpperCase(Locale.ROOT) + " during " + (season == Season.ALL ? "ALL_SEASONS" : season.name());
                            System.out.println("Making graph: " + title);
                            File file = new File(("./output/" + province + "/" + season + "/" + stationsOption.name() + "_" + readingOf.name() + ".png").toLowerCase(Locale.ROOT));
                            Data.Query query = new Data.Query.Builder().readingOf(readingOf).stationsOption(stationsOption).province(province).season(season).build();
                            createChart(file, title, startYear, endYear, query);
                        }
                    }
                }
            })).start();
        }
    }

    private void createChart(File file, String title, int startYear, int endYear, Data.Query query){
        try {
            FileUtils.openOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(
                title,
                "Year",
                "Value",
                createDataset(startYear, endYear, query),
                true, true, true
        );

        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer();
        r.setSeriesPaint(0, Color.BLUE);
        r.setSeriesPaint(1, Color.GREEN);
        r.setSeriesPaint(2, Color.RED);
        r.setSeriesStroke(0, new BasicStroke(2.0f));
        r.setSeriesStroke(1, new BasicStroke(3.0f));
        r.setSeriesStroke(2, new BasicStroke(1.0f));
        r.setBaseShapesVisible(false);
        timeSeriesChart.getXYPlot().setRenderer(r);

        try {
            ChartUtilities.saveChartAsPNG(file, timeSeriesChart, 2000, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private XYDataset createDataset(int startYear, int endYear, Data.Query query) {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        TimeSeries mainValueSeries = new TimeSeries("Value");
        TimeSeries rollingAverageSeries = new TimeSeries(ROLLING_NUMBER + "-Year Rolling Avg");
        TimeSeries linearTrendSeries = new TimeSeries("Linear Trend");

        WeightedObservedPoints allPoints = new WeightedObservedPoints();
        RollingAverage rollingAverage = new RollingAverage();
        for (int i = startYear; i <= endYear; i++) {
            Year year = new Year(i);

            query.setYear(i);
            float mainValue = data.query(query);

            if (mainValue != Float.MIN_VALUE) {
                allPoints.add(i, mainValue);
                rollingAverage.update(mainValue);
                mainValueSeries.add(new TimeSeriesDataItem(year, mainValue));
                if (rollingAverage.getValue() != Float.MIN_VALUE) {
                    rollingAverageSeries.add(new TimeSeriesDataItem(year, rollingAverage.getValue()));
                }
            }
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        ArrayList<WeightedObservedPoint> list = new ArrayList<>(allPoints.toList());

        if (list.size() > 0) {
            double[] coef = fitter.fit(list);
            double b = coef[0],
                    slope = coef[1];
            for (WeightedObservedPoint p : list) {
                linearTrendSeries.add(new TimeSeriesDataItem(new Year((int) p.getX()), p.getX() * slope + b));
            }
        }

        dataset.addSeries(mainValueSeries);
        dataset.addSeries(rollingAverageSeries);
        dataset.addSeries(linearTrendSeries);

        return dataset;
    }

    public static class RollingAverage {
        final ArrayList<Float> numbers;

        RollingAverage() {
            numbers = new ArrayList<>();
        }

        void update(float newValue) {
            numbers.add(newValue);
            if (ROLLING_NUMBER < numbers.size()) {
                numbers.remove(0);
            }
        }

        float getValue() {
            if (numbers.size() < ROLLING_NUMBER) {
                return Float.MIN_VALUE;
            }
            float total = 0;
            for (float f : numbers) {
                total += f;
            }
            return total / ROLLING_NUMBER;
        }
    }
}
