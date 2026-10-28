package io.rea.obd.ui;

public final class NavigationState {
    public static final int DIAGNOSIS = 1;
    public static final int GARAGE = 2;
    public static final int HISTORY = 5;
    public static final int HOME = 4;
    public static final int JOURNAL = 6;
    public static final int LIVE = 0;
    public static final int SETTINGS = 3;

    public static int back(int i) {
        if (i == 0) {
            return 1;
        }
        return i == 6 ? 5 : 4;
    }

    public static int restore(int i) {
        if (i < 0 || i > 6) {
            return 4;
        }
        return i;
    }

    public static int tab(int i) {
        if (i == 0 || i == 1) {
            return 1;
        }
        if (i != 2) {
            return (i == 5 || i == 6) ? 2 : 0;
        }
        return 3;
    }

    private NavigationState() {
    }

    public static int route(int i) {
        return new int[]{4, 1, 5, 2}[i];
    }
}