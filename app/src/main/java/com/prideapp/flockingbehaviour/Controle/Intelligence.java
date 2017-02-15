package com.prideapp.flockingbehaviour.Controle;

import com.prideapp.flockingbehaviour.MainActivity;
import com.prideapp.flockingbehaviour.Model.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;

/**
 * Created by Александр on 12.02.2017.
 */

public class Intelligence {

    private static byte RALLYING_FACTOR = 100;      //divider
    private static byte SEGREGATION_FACTOR = 30;    //distance of personal space
    private static byte ALIGNMENT_FACTOR = 8;       //divider
    private static byte OUT_OF_BOUNDS_FACTOR = 40;  //opposite vector value
    private static byte OUT_OF_OBSTACLE_FACTOR = 5;//opposite vector valur
    private static byte TENDING_FACTOR = 20;       //divider

    private static byte ANTI_RALLYING_FACTOR = -50;

    private static byte MAX_PROXIMITY = 20;
    private static int MAX_VELOCITY = 50;


    public static void moveAllUnitsAsync(ArrayList<Unit> units,
                                         HashMap<Point2D, Integer> circleObstacles,
                                         Point2D aim)
            throws InterruptedException {

        List<Thread> intelligenceThreads = new ArrayList<>(MainActivity.UNITS_AMOUNT);
        for (Unit unit : units) {

            intelligenceThreads.add(new Thread(new MyRunnable(unit, units, circleObstacles, aim)));
        }
        for (Thread thread : intelligenceThreads) {
            thread.start();
        }
        for (Thread thread : intelligenceThreads) {
            thread.join();
        }
    }

    //rule1
    private static Vector2D rallying(ArrayList<Unit> units, Unit certainUnit) {

        Vector2D c = findCenterOfMassForUnit(units, certainUnit);

        return c.minus(certainUnit.getPosition())
                .times((double) 1 / RALLYING_FACTOR);
    }

    private static Vector2D findCenterOfMassForUnit(ArrayList<Unit> units, Unit certainUnit) {
        Vector2D center = new Vector2D(0, 0);

        for (Unit unit : units)
            if (!unit.equals(certainUnit))
                center = center.plus(unit.getPosition());

        return center.times((double) 1 / (units.size() - 1));
    }


    //rule2
    private static Vector2D segregation(ArrayList<Unit> units, Unit certainUnit) {
        Vector2D displacement = new Vector2D(0, 0);

        for (Unit unit : units)
            if (!unit.equals(certainUnit))
                if (distance(certainUnit.getPosition(), unit.getPosition()) < SEGREGATION_FACTOR)
                    displacement = displacement.minus(unit.getPosition()
                            .minus(certainUnit.getPosition()));

        return displacement;
    }

    private static double distance(Vector2D p1, Vector2D p2) {
        return Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + Math.pow(p2.getY() - p1.getY(), 2));
    }

    //rule3
    private static Vector2D alignment(ArrayList<Unit> units, Unit certainUnit) {
        return findAverageVelocityForUnit(units, certainUnit)
                .minus(certainUnit.getVelocity())
                .times((double) 1 / ALIGNMENT_FACTOR);
    }

    private static Vector2D findAverageVelocityForUnit(ArrayList<Unit> units, Unit certainUnit) {
        Vector2D averageVelocity = new Vector2D(0, 0);

        for (Unit unit : units)
            if (!unit.equals(certainUnit))
                averageVelocity = averageVelocity.plus(unit.getVelocity());

        return averageVelocity.times((double) 1 / (units.size() - 1));
    }


    //rule4
    private static Vector2D boundPosition(Unit unit) {
        Vector2D v = new Vector2D(0, 0);

        if (unit.getPosition().getX() < MAX_PROXIMITY)
            v = v.plus(new Vector2D(OUT_OF_BOUNDS_FACTOR, 0));
        else if (unit.getPosition().getX() > MainActivity.WIDTH - MAX_PROXIMITY)
            v = v.plus(new Vector2D(-OUT_OF_BOUNDS_FACTOR, 0));

        if (unit.getPosition().getY() < MAX_PROXIMITY)
            v = v.plus(new Vector2D(0, OUT_OF_BOUNDS_FACTOR));
        else if (unit.getPosition().getY() > MainActivity.HEIGHT - MAX_PROXIMITY)
            v = v.plus(new Vector2D(0, -OUT_OF_BOUNDS_FACTOR));

        return v;
    }

    //rule5
    private static Vector2D obstacleBypass(Unit unit, HashMap<Point2D, Integer> circleObstacles) {

        Vector2D v = new Vector2D(0, 0);

        for (Map.Entry<Point2D, Integer> circle : circleObstacles.entrySet())
            if (distance(unit.getPosition(), new Vector2D(circle.getKey())) - circle.getValue()
                    < MAX_PROXIMITY) {

                Vector2D outOfObstacleVector = new Vector2D(unit.getPosition().getX() - circle.getKey().getX(),
                        unit.getPosition().getY() - circle.getKey().getY());

                Vector2D outOfObstacleNormalizedVector = outOfObstacleVector
                        .times((double) 1 /
                                (distance(unit.getPosition(), new Vector2D(circle.getKey()))
                                        - circle.getValue()));

                v = v.plus(outOfObstacleNormalizedVector.times(OUT_OF_OBSTACLE_FACTOR));
            }

        return v;
    }

    //condition1
    private static Vector2D tendToPlace(Unit unit, Point2D aim) {
        if(aim.getX() > 0 || aim.getY() > 0)
            return (new Vector2D(aim)).minus(unit.getPosition()).times((double)1/TENDING_FACTOR);
        else
            return new Vector2D(0, 0);
    }


    private static void limitVelocity(Unit unit) {
        if (distance(new Vector2D(0, 0), unit.getVelocity()) > MAX_VELOCITY)
            unit.setVelocity(unit.getVelocity()
                    .times((double) 1 / distance(new Vector2D(0, 0), unit.getVelocity()))
                    .times(MAX_VELOCITY));
    }

    static class MyRunnable implements Runnable {
        private Unit unit;
        private ArrayList<Unit> units;
        private HashMap<Point2D, Integer> circleObstacles;
        private Point2D aim;
        private byte rallyingCoefficient;

        public MyRunnable(Unit unit, ArrayList<Unit> units,
                          HashMap<Point2D, Integer> circleObstacles,
                          Point2D aim) {
            this.unit = unit;
            this.units = units;
            this.circleObstacles = circleObstacles;
            this.aim = aim;
        }

        @Override
        public void run() {
            unit.setVelocity(unit.getVelocity()
                    .plus(rallying(units, unit))
                    .plus(segregation(units, unit))
                    .plus(alignment(units, unit))
                    .plus(boundPosition(unit))
                    .plus(obstacleBypass(unit, circleObstacles))
                    .plus(tendToPlace(unit, aim)));

            limitVelocity(unit);

            unit.update(MainActivity.DELTA_TIME_MILLIS);
        }
    }
}
