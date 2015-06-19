package test;

import thu.ic.collavoid.commons.planners.Vector2;

import java.util.*;

public class testSort {
    private static double footprint_radius_ = 0.17;
    private static List<Vector2> footprint_original = new ArrayList<>();
    private static List<Vector2> footprint_pose = new ArrayList<>();
    private static Vector2[] footprint_sum;
    static long start, end, sum, cnt;

    public static void main(String[] args) {
        Random random = new Random();
        for (int i = 0; i < 500; i++) {
            footprint_original.add(new Vector2(random.nextDouble() * 5, random.nextDouble() * 5));
        }

        for (int i = 0; i < 500; i++) {
            footprint_pose.add(new Vector2(random.nextDouble() * 2.5, random.nextDouble() * 2.5));
        }

        footprint_sum = new Vector2[footprint_pose.size() * footprint_original.size()];
        int idx = 0;
        for (Vector2 pt : footprint_original) {
            for (Vector2 ptp : footprint_pose) {
                footprint_sum[idx++] = new Vector2(pt.getX() + ptp.getX(), pt.getY() + ptp.getY());
            }
        }
        cnt = 100;
        sum = 0;
        for (int i = 0; i < cnt; i++) {
            Vector2[] footprint_sum_bak = new Vector2[footprint_sum.length];
            for (int j = 0; j < footprint_sum.length; j++) {
                footprint_sum_bak[j] = footprint_sum[j];
            }
            start = System.currentTimeMillis();
            Arrays.sort(footprint_sum, new VectorsLexigraphicComparator());
            end = System.currentTimeMillis();
            sum = sum + end - start;
            footprint_sum = footprint_sum_bak;
        }

        System.out.println("Serial sort: " + sum / (double) cnt);

        sum = 0;
        for (int i = 0; i < cnt; i++) {
            Vector2[] footprint_sum_bak = new Vector2[footprint_sum.length];
            for (int j = 0; j < footprint_sum.length; j++) {
                footprint_sum_bak[j] = footprint_sum[j];
            }
            start = System.currentTimeMillis();
            Arrays.parallelSort(footprint_sum, new VectorsLexigraphicComparator());
            end = System.currentTimeMillis();
            sum = sum + end - start;
            footprint_sum = footprint_sum_bak;
        }

        System.out.println("Parallel sort: " + sum / (double) cnt);


    }

    public static class VectorsLexigraphicComparator implements Comparator<Vector2> {

        public int compare(Vector2 c1, Vector2 c2) {
            if (null == c1 || null == c2) {
                return -1;
            }

            if (c1.getX() < c2.getX() || (c1.getX() == c2.getX() && c1.getY() < c2.getY())) {
                return -1;
            } else if (c1.getX() == c2.getX() && c1.getY() == c2.getY()) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}
