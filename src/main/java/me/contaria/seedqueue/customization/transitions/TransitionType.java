package me.contaria.seedqueue.customization.transitions;

import static java.lang.Math.*;

public enum TransitionType {
    LINEAR,
    EASE_IN_SINE,
    EASE_OUT_SINE,
    EASE_IN_OUT_SINE,
    EASE_IN_CUBIC,
    EASE_OUT_CUBIC,
    EASE_IN_OUT_CUBIC,
    EASE_IN_QUINT,
    EASE_OUT_QUINT,
    EASE_IN_OUT_QUINT,
    EASE_IN_CIRC,
    EASE_OUT_CIRC,
    EASE_IN_OUT_CIRC,
    EASE_IN_QUAD,
    EASE_OUT_QUAD,
    EASE_IN_OUT_QUAD,
    EASE_IN_QUART,
    EASE_OUT_QUART,
    EASE_IN_OUT_QUART,
    EASE_IN_EXPO,
    EASE_OUT_EXPO,
    EASE_IN_OUT_EXPO;

    public static double get(TransitionType type, double p) {
        switch (type) {
            case EASE_IN_QUAD:
                return pow(p, 2);
            case EASE_OUT_QUAD:
                return 1 - pow(1 - p, 2);
            case EASE_IN_OUT_QUAD:
                return p < 0.5 ? 2 * pow(p, 2) : 1 - pow(-2 * p + 2, 2) / 2;
            case EASE_IN_CUBIC:
                return pow(p, 3);
            case EASE_OUT_CUBIC:
                return 1 - pow(1 - p, 3);
            case EASE_IN_OUT_CUBIC:
                return p < 0.5 ? 4 * pow(p, 3) : 1 - pow(-2 * p + 2, 3) / 2;
            case EASE_IN_QUART:
                return pow(p, 4);
            case EASE_OUT_QUART:
                return 1 - pow(1 - p, 4);
            case EASE_IN_OUT_QUART:
                return p < 0.5 ? 8 * pow(p, 4) : 1 - pow(-2 * p + 2, 4) / 2;
            case EASE_IN_QUINT:
                return pow(p, 5);
            case EASE_OUT_QUINT:
                return 1 - pow(1 - p, 5);
            case EASE_IN_OUT_QUINT:
                return p < 0.5 ? 16 * pow(p, 5) : 1 - pow(-2 * p + 2, 5) / 2;
            case EASE_IN_SINE:
                return 1 - cos((p * PI) / 2);
            case EASE_OUT_SINE:
                return sin((p * PI) / 2);
            case EASE_IN_OUT_SINE:
                return -(cos(PI * p) - 1) / 2;
            case EASE_IN_CIRC:
                return 1 - sqrt(1 - pow(p, 2));
            case EASE_OUT_CIRC:
                return sqrt(1 - pow(p - 1, 2));
            case EASE_IN_OUT_CIRC:
                return p < 0.5
                        ? (1 - sqrt(1 - pow(2 * p, 2))) / 2
                        : (sqrt(1 - pow(-2 * p + 2, 2)) + 1) / 2;
            case EASE_IN_EXPO:
                return p == 0 ? 0 : pow(2, 10 * p - 10);
            case EASE_OUT_EXPO:
                return p == 1 ? 1 : 1 - pow(2, -10 * p);
            case EASE_IN_OUT_EXPO:
                return p == 0 ? 0
                        : p == 1 ? 1
                        : p < 0.5 ? pow(2, 20 * p - 10) / 2
                        : (2 - pow(2, -20 * p + 10)) / 2;
            case LINEAR:
            default:
                return p;
        }
    }
}
