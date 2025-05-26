import java.io.Serializable;

public class CalculationObject implements Serializable {
    private int x;
    private int y;

    public CalculationObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
