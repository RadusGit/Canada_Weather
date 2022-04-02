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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class Graph {
    final Data data;

    Graph() {
        data = new Data();
    }

    public static void main(String[] args) {
        Graph g = new Graph();
//        g.getData().download();
//        g.getData().load();
//        g.pewpew();
    }

    public Data getData() {
        return data;
    }

    private void pewpew() {
        for (ReadingOf readingOf : ReadingOf.values()) {
            for (Province province : Province.values()) {
                for (Season season : Season.values()) {
                    for (StationsOption stationsOption : StationsOption.values()) {
                        String title = stationsOption.name() + " " + readingOf.name() + " in " + province.toString().toUpperCase(Locale.ROOT) + " during " + (season == Season.ALL ? "ALL_SEASONS" : season.name());
                        File file = new File(("./output/" + province + "/" + season + "/" + stationsOption.name() + "_" + readingOf.name() + ".png").toLowerCase(Locale.ROOT));

                        try {
                            FileUtils.openOutputStream(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(
                                title,
                                "Year",
                                "Value",
                                createDataset(1900, 2021, readingOf, province, season, stationsOption),
                                true, true, true
                        );

                        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer();
                        r.setSeriesPaint(0, Color.BLUE);
                        r.setSeriesPaint(1, Color.GREEN);
                        r.setSeriesPaint(2, Color.RED);
                        r.setSeriesStroke(0, new BasicStroke(2.0f));
                        r.setSeriesStroke(1, new BasicStroke(2.0f));
                        r.setSeriesStroke(2, new BasicStroke(1.0f));
                        r.setBaseShapesVisible(false);
                        timeSeriesChart.getXYPlot().setRenderer(r);

                        try {
                            ChartUtilities.saveChartAsPNG(file, timeSeriesChart, 2000, 1000);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    }

    private XYDataset createDataset(int startYear, int endYear, ReadingOf readingOf, Province province, Season season, StationsOption stationsOption) {

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        TimeSeries mainValueSeries = new TimeSeries("Raw Value");
        TimeSeries rollingAverageSeries = new TimeSeries("7-Year Rolling Avg");
        TimeSeries linearTrendSeries = new TimeSeries("Linear Trend");

        WeightedObservedPoints allPoints = new WeightedObservedPoints();
        RollingAverage rollingAverage = new RollingAverage(7);
        for (int i = startYear; i <= endYear; i++) {
            Year year = new Year(i);

            float mainValue = data.query(new Data.Query.Builder()
                    .readingOf(readingOf).year(i).province(province).season(season).stationsOption(stationsOption)
                    .build());

            if (mainValue != Float.MIN_VALUE) {
                allPoints.add((float) i, mainValue);
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
            float b = (float) coef[0],
                    slope = (float) coef[1];
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
        final int rollingNumber;
        final ArrayList<Float> numbers;

        RollingAverage(int rollingNumber) {
            numbers = new ArrayList<>();
            this.rollingNumber = rollingNumber;
        }

        void update(float newValue) {
            numbers.add(newValue);
            if (rollingNumber < numbers.size()) {
                numbers.remove(0);
            }
        }

        float getValue() {
            if (numbers.size() < rollingNumber) {
                return Float.MIN_VALUE;
            }
            float total = 0;
            for (float f : numbers) {
                total += f;
            }
            return total / rollingNumber;
        }
    }
}
