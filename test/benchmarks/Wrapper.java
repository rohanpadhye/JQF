package benchmarks;

public class Wrapper {
    private String data;
    public static void main(String args[]) {
        try {
            Wrapper w = new Wrapper(args[0]);
            System.out.println(w.getData());
        } catch (Exception e) {
           System.err.println(e.getClass().getCanonicalName() + ": " + e.getMessage());
        }
    }

    public Wrapper(String x) {
        this.data = x;
    }

    public String getData() {
        return this.data;
    }
}
