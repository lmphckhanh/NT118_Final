package com.example.afinal;

import java.util.List;

public class OsrmResponse {
    private List<Route> routes;

    public List<Route> getRoutes() {
        return routes;
    }

    public static class Route {
        private List<Leg> legs;

        public List<Leg> getLegs() {
            return legs;
        }
    }

    public static class Leg {
        private List<Step> steps;

        public List<Step> getSteps() {
            return steps;
        }
    }

    public static class Step {
        private double[] geometry;

        public double[] getGeometry() {
            return geometry;
        }
    }
}
