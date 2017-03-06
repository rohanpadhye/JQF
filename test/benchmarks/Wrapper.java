package benchmarks;

import jwig.logging.SingleSnoop;

public class Wrapper {
    static {
        SingleSnoop.startSnooping();
    }
    private String data;
    public static void main(String args[]) {
        try {
            Wrapper w = args.length > 0 ? new Wrapper(args[0]) : new Wrapper();
            String s = w.getData();
            if (s.equals("ERROR")) {
                throw new Exception("Data is errornous");
            }
            System.out.println(s);
        } catch (Exception e) {
           System.err.println(e.getClass().getCanonicalName() + ": " + e.getMessage());
        }
    }

    public Wrapper(String x) {
        this.data = x;
    }

    public Wrapper() {

    }

    public String getData() {
        return this.data.toString();
    }
}
